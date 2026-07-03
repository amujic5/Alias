package hr.azzi.socialgames.alias.online

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.serializedName
import hr.azzi.socialgames.alias.ai.AuthService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * The Online Play view-model — Android port of iOS `OnlineStore.swift`. Ties together
 * auth, the Firestore room listener, the Stream call and the round timer, and exposes
 * everything the Compose screens need. All Firestore writes funnel through the repo.
 */
class OnlineStore(app: Application) : AndroidViewModel(app) {

    private val appCtx get() = getApplication<Application>()
    private val repo = OnlineRoomRepository()
    val video = OnlineVideoManager(appCtx.applicationContext)

    private val _room = MutableStateFlow<OnlineRoom?>(null)
    val room: StateFlow<OnlineRoom?> = _room.asStateFlow()
    private val _words = MutableStateFlow<List<String>>(emptyList())
    val words: StateFlow<List<String>> = _words.asStateFlow()
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _dismissed = MutableStateFlow(false)
    val dismissed: StateFlow<Boolean> = _dismissed.asStateFlow()
    private val _myRooms = MutableStateFlow<List<OnlineRoom>>(emptyList())
    val myRooms: StateFlow<List<OnlineRoom>> = _myRooms.asStateFlow()

    /** One-shot: fire the round-end interstitial (collected by the Activity, needs an Activity ctx). */
    private val _showAd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showAd: SharedFlow<Unit> = _showAd

    private var observeJob: Job? = null
    private var timerJob: Job? = null
    private var editJob: Job? = null
    private var myAgoraUid = 0
    private var roundEndPosted = false
    private var lastClockSecond = -1
    private var editedResults: List<OnlineMarkedWord>? = null   // local edits in the review sheet

    val myUid: String? get() = AuthService.uid

    init {
        video.tokenProvider = { fetchStreamToken() }
    }

    private suspend fun fetchStreamToken(): String? = runCatching {
        val res = FirebaseFunctions.getInstance().getHttpsCallable("getStreamVideoToken").call().await()
        @Suppress("UNCHECKED_CAST")
        (res.data as? Map<String, Any?>)?.get("token") as? String
    }.getOrNull()

    // MARK: Derived

    fun amHost(): Boolean = _room.value?.isHost(myUid) ?: false
    fun myRole(): OnlineRole = _room.value?.role(myUid) ?: OnlineRole.SPECTATOR
    fun amExplainer(): Boolean = myRole() == OnlineRole.EXPLAINER
    fun amGuesser(): Boolean = myRole() == OnlineRole.GUESSER

    /** The word currently in play (explainer + spectators see it; guesser doesn't). */
    fun currentWord(): String? {
        val cursor = _room.value?.wordCursor ?: return null
        return _words.value.getOrNull(cursor)
    }
    fun secondsRemaining(): Int = _room.value?.secondsRemaining(_now.value) ?: 0

    // MARK: Create / join

    fun createRoom(deck: BoardGame, languageCode: String, languageName: String,
                   maxPlayers: Int, goalScore: Int, roundSeconds: Int) {
        viewModelScope.launch {
            val uid = myUid
            if (uid == null) { _error.value = errorText(OnlineException(OnlineError.NOT_SIGNED_IN)); return@launch }
            val name = AuthService.currentName()
            _busy.value = true
            try {
                val words = OnlineWordSource.frozenWords(appCtx, deck.id.serializedName(), languageCode)
                if (words.isEmpty()) { _error.value = appCtx.getString(R.string.online_err_nowords); return@launch }
                myAgoraUid = makeAgoraUid()
                repeat(8) {
                    val code = OnlineRoomCode.generate()
                    val room = OnlineRoom(
                        id = code, code = code, hostId = uid, hostName = name,
                        deckId = deck.id.serializedName(), deckName = resolveDeckName(deck),
                        language = languageName, languageCode = languageCode,
                        maxPlayers = maxPlayers, goalScore = goalScore, roundSeconds = roundSeconds,
                        wordCount = words.size,
                        teams = OnlineTeams.make(maxPlayers / 2) { letter -> appCtx.getString(R.string.online_team_named, letter) },
                        players = listOf(OnlinePlayer(id = uid, name = name, agoraUid = myAgoraUid, teamId = 0)),
                        pastMemberIds = listOf(uid),
                    )
                    try {
                        repo.createRoom(room, words)
                        OnlineAnalytics.roomCreated(appCtx)
                        enter(code)
                        return@launch
                    } catch (e: OnlineException) {
                        if (e.error == OnlineError.CODE_TAKEN) return@repeat  // collision — new code
                        _error.value = errorText(e); return@launch
                    }
                }
                _error.value = errorText(OnlineException(OnlineError.CODE_TAKEN))  // gave up after retries
            } catch (e: Exception) {
                _error.value = errorText(e)
            } finally {
                _busy.value = false
            }
        }
    }

    /** Invokes [onResult] with true on success so the caller can route into the lobby. */
    fun joinRoom(rawCode: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val uid = myUid
            if (uid == null) { _error.value = errorText(OnlineException(OnlineError.NOT_SIGNED_IN)); onResult(false); return@launch }
            val code = rawCode.uppercase().trim()
            _busy.value = true
            try {
                val name = AuthService.currentName()
                myAgoraUid = makeAgoraUid()
                repo.joinRoom(code, uid, name, myAgoraUid)
                OnlineAnalytics.roomJoined(appCtx)
                enter(code)
                onResult(true)
            } catch (e: Exception) {
                _error.value = errorText(e)
                onResult(false)
            } finally {
                _busy.value = false
            }
        }
    }

    fun loadMyRooms() {
        viewModelScope.launch {
            val uid = myUid ?: run { _myRooms.value = emptyList(); return@launch }
            _myRooms.value = repo.myRooms(uid)
        }
    }

    // MARK: Session lifecycle

    private fun enter(code: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch { repo.observe(code).collect { handle(it) } }
        viewModelScope.launch { runCatching { _words.value = repo.fetchWords(code) } }
        val name = _room.value?.hostName ?: AuthService.displayName ?: "You"
        myUid?.let { video.start(it, name, code) }
        startTimer()
    }

    private fun handle(room: OnlineRoom?) {
        if (room == null) return
        val prev = _room.value
        val prevPhase = prev?.roundPhase
        val prevStatus = prev?.status
        val wasReview = prevPhase == OnlineRoundPhase.REVIEW

        // Interstitial: preload when the round starts (→ active), present for EVERYONE
        // the moment it ends (active → review, countdown at 0).
        if (prevPhase != OnlineRoundPhase.ACTIVE && room.roundPhase == OnlineRoundPhase.ACTIVE) {
            OnlineInterstitial.preload(appCtx)
        }
        if (prevPhase == OnlineRoundPhase.ACTIVE && room.roundPhase == OnlineRoundPhase.REVIEW) {
            _showAd.tryEmit(Unit)
        }

        _room.value = room

        // Analytics — room lifecycle, gated on host so each transition logs exactly once.
        if (amHost()) {
            if (prevStatus == OnlineStatus.LOBBY && room.status == OnlineStatus.PLAYING)
                OnlineAnalytics.roomStarted(appCtx, room.maxPlayers)
            if (prevStatus != OnlineStatus.FINISHED && room.status == OnlineStatus.FINISHED)
                OnlineAnalytics.roomFinished(appCtx)
            if (prevStatus != OnlineStatus.CANCELLED && room.status == OnlineStatus.CANCELLED)
                OnlineAnalytics.roomCancelled(appCtx)
        }

        if (room.roundPhase != OnlineRoundPhase.REVIEW) editedResults = null
        else if (!wasReview) editedResults = null
        if (room.roundPhase != OnlineRoundPhase.ACTIVE) roundEndPosted = false
        // Terminal: host cancelled/closed → everyone tears down and leaves.
        if (room.status == OnlineStatus.CANCELLED) { teardown(); _dismissed.value = true }
    }

    fun leaveAndTeardown() {
        val code = _room.value?.code
        val uid = myUid
        if (code != null && uid != null && _room.value?.status == OnlineStatus.LOBBY) {
            viewModelScope.launch { runCatching { repo.leave(code, uid) } }
        }
        teardown()
        _dismissed.value = true
    }

    private fun teardown() {
        observeJob?.cancel(); observeJob = null
        timerJob?.cancel(); timerJob = null
        video.leave()
    }

    fun clearError() { _error.value = null }

    // MARK: Host actions

    fun startGame() = launchOp { repo.startGame(it) }
    fun shuffleTeams() = launchOp { repo.shuffleTeams(it) }
    fun movePlayer(uid: String, teamId: Int) { if (amHost()) launchOp { repo.assignTeam(it, uid, teamId) } }
    fun stopGame() = launchOp { repo.cancel(it) }

    // MARK: Explainer actions

    fun startRound() = launchOp { repo.startRound(it) }

    /** Mark the current word correct/skipped, advance the shared cursor. */
    fun mark(correct: Boolean) {
        val room = _room.value ?: return
        if (!amExplainer()) return
        val word = currentWord() ?: return
        val results = room.roundResults + OnlineMarkedWord(word, correct)
        val cursor = minOf(room.wordCursor + 1, maxOf(0, _words.value.size - 1))
        launchOp { repo.setRoundResults(it, results, cursor) }
    }

    fun endRound() = launchOp { repo.endRound(it) }

    // Review / edit answers

    /** The round's words with the explainer's in-progress edits applied. */
    fun reviewResults(): List<OnlineMarkedWord> = editedResults ?: _room.value?.roundResults ?: emptyList()

    fun toggleAnswer(word: String) {
        val results = reviewResults().map { if (it.word == word) it.copy(correct = !it.correct) else it }
        editedResults = results
        _room.value = _room.value   // nudge collectors (optimistic local flip)
        scheduleEditSync()
    }

    /** Push the explainer's in-progress edits 300ms after the last toggle (debounced). */
    private fun scheduleEditSync() {
        editJob?.cancel()
        editJob = viewModelScope.launch {
            delay(300)
            val code = _room.value?.code ?: return@launch
            if (_room.value?.roundPhase != OnlineRoundPhase.REVIEW) return@launch
            val edits = editedResults ?: return@launch
            val cursor = _room.value?.wordCursor ?: 0
            runCatching { repo.setRoundResults(code, edits, cursor) }.onFailure { _error.value = errorText(it) }
        }
    }

    /** Explainer: persist any answer edits and move everyone to the standings view. */
    fun saveAnswers() {
        val code = _room.value?.code ?: return
        editJob?.cancel()
        val edits = editedResults
        val cursor = _room.value?.wordCursor ?: 0
        viewModelScope.launch {
            try {
                if (edits != null) repo.setRoundResults(code, edits, cursor)
                repo.saveAnswers(code)
            } catch (e: Exception) { _error.value = errorText(e) }
        }
    }

    fun finishRound() { OnlineAnalytics.roundFinished(appCtx); launchOp { repo.finishRound(it) } }
    fun editAnswers() = launchOp { repo.editAnswers(it) }

    // MARK: Local A/V

    fun toggleMic() = video.setMic(!video.micOn.value)
    fun toggleCamera() = video.setCamera(!video.cameraOn.value)
    fun switchCamera() = video.switchCamera()

    // MARK: Helpers

    private fun makeAgoraUid() = Random.nextInt(1, Int.MAX_VALUE)

    private fun resolveDeckName(deck: BoardGame): String {
        val resId = appCtx.resources.getIdentifier(deck.name, "string", appCtx.packageName)
        return if (resId != 0) appCtx.getString(resId) else deck.name
    }

    /** Runs a repo mutation keyed by the current room code, surfacing errors. */
    private fun launchOp(op: suspend (String) -> Unit) {
        val code = _room.value?.code ?: return
        viewModelScope.launch {
            try { op(code) } catch (e: Exception) { _error.value = errorText(e) }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) { delay(250); tick() }
        }
    }

    private fun tick() {
        _now.value = System.currentTimeMillis()
        val room = _room.value ?: return
        if (room.status != OnlineStatus.PLAYING || room.roundPhase != OnlineRoundPhase.ACTIVE) return
        val s = secondsRemaining()
        if (s != lastClockSecond) lastClockSecond = s
        // The explainer's device is the single writer that ends the round when time runs out.
        if (amExplainer() && s == 0 && !roundEndPosted) {
            roundEndPosted = true
            endRound()
        }
    }

    private fun errorText(t: Throwable): String = when (t) {
        is OnlineException -> appCtx.getString(
            when (t.error) {
                OnlineError.NOT_FOUND -> R.string.online_err_notfound
                OnlineError.EXPIRED -> R.string.online_err_expired
                OnlineError.CLOSED -> R.string.online_err_closed
                OnlineError.FULL -> R.string.online_err_full
                OnlineError.NOT_SIGNED_IN -> R.string.online_err_notsignedin
                OnlineError.CODE_TAKEN -> R.string.online_err_codetaken
            }
        )
        else -> t.message ?: appCtx.getString(R.string.online_err_notfound)
    }

    override fun onCleared() {
        super.onCleared()
        teardown()
    }
}
