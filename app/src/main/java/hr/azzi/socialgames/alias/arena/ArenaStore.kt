package hr.azzi.socialgames.alias.arena

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import hr.azzi.socialgames.alias.ai.AuthService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Drives one language's stream of Arena rounds.
 *
 * Android port of iOS `ArenaStore.swift`.
 *
 * The Arena has no "start" button and no room to join. Rounds run on their own, back to
 * back, whether anybody is watching — so this object's job is to work out *which* round
 * is happening, count down against it, and hand the result to the server when it ends.
 *
 * Three rules shape the whole file:
 *
 * * **The server owns the clock.** Every countdown comes from [ArenaClock], never from
 *   the device. A phone whose clock is two minutes fast would otherwise start playing
 *   during the break and submit into a round that had already been scored.
 * * **The round owns its timings.** `playSec`, the grace window and the next start are
 *   frozen into the round document when it is created, so a config change cannot move
 *   the finish line under a round in progress.
 * * **Rounds chain by id.** A round id is `"{lang}_{index}"`, so once the first round is
 *   found the next one is a direct document read rather than a fresh query — cheaper, and
 *   it cannot skip a round the way a re-run query racing the clock can.
 */
class ArenaStore(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "ArenaStore"
        const val REGION = "europe-west1"

        /**
         * A player may still enter while at least this many seconds of play remain.
         * Overridden by the round document when it carries the value; the fallback
         * matches the server's own default.
         */
        const val DEFAULT_LATE_JOIN_MIN_REMAINING_SEC = 40

        /**
         * Below this there is no point starting: a dozen seconds is not a round, it is a
         * glance at a word. Between here and the automatic threshold the player may opt
         * in themselves — see [forceJoinSeconds].
         */
        const val FORCE_JOIN_MIN_SEC = 30

        /**
         * How early the round's words may be read, matching the rule on `deal/words`.
         *
         * Changing this alone does nothing: the server decides, and a client that asks
         * sooner is refused. The two numbers have to move together.
         */
        const val DEAL_LEAD_MS = 10_000L

        /**
         * The board is fetched this long after submissions close: the server has five
         * seconds to score, and one more is margin.
         */
        const val BOARD_FETCH_DELAY_MS = 6_000L

        /**
         * How long after the board was due the client keeps waiting before deciding there
         * will never be one.
         *
         * A round nobody played is never scored and never gets a board, so something has
         * to end the wait. Generous, because the alternative failure — giving up on a
         * board that was merely slow — shows an empty leaderboard to somebody who just
         * played.
         */
        private const val BOARD_GIVE_UP_MS = 25_000L

        private const val PREFS = "alias_arena"
        private const val KEY_LANG = "arena_language"
        private const val KEY_MODE = "arena_mode"

        /**
         * Languages the Arena runs rounds in.
         *
         * These are the document ids under `arenaDecks`, which the ingest script writes
         * in lower case. Bosnian is absent because it has no clue file, so no deck is
         * written for it.
         */
        val LANGUAGES = listOf("cro", "eng", "esp", "fra", "ger", "ita", "slo", "srb")

        /** Where a round is in its own cycle. Pure, so it can be reasoned about alone. */
        fun phaseOf(round: ArenaRound, now: Long): ArenaPhase = when {
            now < round.startsAtMs ->
                ArenaPhase.Lobby(maxOf(0, ((round.startsAtMs - now + 999) / 1000).toInt()))
            now < round.endsAtMs ->
                ArenaPhase.Playing(maxOf(0, ((round.endsAtMs - now + 999) / 1000).toInt()))
            else ->
                ArenaPhase.Results(maxOf(0, ((round.nextStartsAtMs - now + 999) / 1000).toInt()))
        }
    }

    private val appCtx get() = getApplication<Application>()
    private val prefs by lazy { appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance(REGION)

    // MARK: Published state

    private val _phase = MutableStateFlow<ArenaPhase>(ArenaPhase.Idle)
    val phase: StateFlow<ArenaPhase> = _phase.asStateFlow()

    private val _round = MutableStateFlow<ArenaRound?>(null)
    val round: StateFlow<ArenaRound?> = _round.asStateFlow()

    private val _board = MutableStateFlow<List<ArenaBoardEntry>>(emptyList())
    val board: StateFlow<List<ArenaBoardEntry>> = _board.asStateFlow()

    /**
     * Whether [board] is this round's final board, fetched once scoring had time to
     * finish. Until then the results screen shows a countdown, never partial rows.
     */
    private val _boardLoaded = MutableStateFlow(false)
    val boardLoaded: StateFlow<Boolean> = _boardLoaded.asStateFlow()

    /** Seconds until the board is fetched, for the results screen's countdown. */
    private val _secondsToResults = MutableStateFlow(0)
    val secondsToResults: StateFlow<Int> = _secondsToResults.asStateFlow()

    /**
     * When the live round ends, in this device's own time — the server's `endsAtMs`
     * translated through the clock offset. Handed to the round engine so its timer
     * measures the round that is actually running rather than a fresh full length.
     */
    private val _playDeadlineMs = MutableStateFlow<Long?>(null)
    val playDeadlineMs: StateFlow<Long?> = _playDeadlineMs.asStateFlow()

    /** How many played the most recent round that has a published board. */
    private val _playersNow = MutableStateFlow(0)
    val playersNow: StateFlow<Int> = _playersNow.asStateFlow()

    /** How many are expected in the round about to start. See [estimateLobby]. */
    private val _playersInLobby = MutableStateFlow(0)
    val playersInLobby: StateFlow<Int> = _playersInLobby.asStateFlow()

    /** Whether there is still enough of the round left to be worth entering. */
    private val _canJoinNow = MutableStateFlow(false)
    val canJoinNow: StateFlow<Boolean> = _canJoinNow.asStateFlow()

    /**
     * Seconds left, when the round is past the automatic threshold but still worth
     * offering — null otherwise.
     *
     * The server does not enforce the late-join rule at all; it takes any submission
     * inside the window. So this is not a loophole, it is the rule saying what it really
     * is: below forty seconds you are not put in automatically, because the result would
     * not stand next to a full round — but you may decide that for yourself, and the
     * button says how much is left so the choice is informed.
     */
    private val _forceJoinSeconds = MutableStateFlow<Int?>(null)
    val forceJoinSeconds: StateFlow<Int?> = _forceJoinSeconds.asStateFlow()

    /**
     * Set when the Arena could not get a session at all.
     *
     * Not the same as "playing as a guest": a guest has a real uid and everything works.
     * This is nobody — the anonymous provider is off, or the device is offline on a cold
     * start — and every read the Arena makes is refused.
     */
    private val _needsSignIn = MutableStateFlow(false)
    val needsSignIn: StateFlow<Boolean> = _needsSignIn.asStateFlow()

    /**
     * Which half of the Arena the player is in. Changing it drops the board so the other
     * mode's is fetched; the rounds themselves are shared by both modes.
     */
    private val _mode = MutableStateFlow(savedMode())
    val mode: StateFlow<ArenaMode> = _mode.asStateFlow()

    /**
     * Which language's rounds are being played, as a deck id (`"cro"`, `"eng"`).
     *
     * A language is a separate stream of rounds, not a filter on one: changing it means
     * leaving the room mid-cycle and joining another that is at a different point in its
     * own. So the round, the board and the countdown all go, and the new language's
     * current round is found from scratch.
     */
    private val _lang = MutableStateFlow(preferredLanguage())
    val lang: StateFlow<String> = _lang.asStateFlow()

    // MARK: Private state

    private var ticker: Job? = null
    private var boardListener: ListenerRegistration? = null
    private var boardListenerKey: String? = null
    private val submittedRoundIds = mutableSetOf<String>()
    private var advancing = false
    private var dealingRoundId: String? = null

    /**
     * The next round, fetched and dealt while the current one is still on screen.
     *
     * Without this the pre-arm never fires. The store holds the finished round all
     * through the break — that is what the results screen is showing — and only picks up
     * the next one at the instant it starts, which is exactly too late to have read its
     * words early. So the next round is loaded in the tail of the break, its hand with it,
     * and handed over complete.
     */
    private var pendingRound: ArenaRound? = null
    private var pendingRoundId: String? = null

    /**
     * When the last round lookup ran, so a language with no scheduled rounds is not
     * queried four times a second for as long as the screen is open.
     */
    private var lastLookupMs = 0L

    /**
     * Humans seen in the last published board, kept across rounds so the lobby has
     * something real to base its estimate on.
     */
    private var lastHumanCount = 0

    override fun onCleared() {
        super.onCleared()
        ticker?.cancel()
        boardListener?.remove()
    }

    // MARK: Entering and leaving

    /**
     * Signs in if needed, syncs the clock, finds the live round and starts counting.
     *
     * Safe to call again — re-entering an Arena that is already running just restarts the
     * tick, which is what returning from the round screen does.
     */
    suspend fun enter() {
        if (AuthService.uid == null) {
            // Anonymous play is deliberate: the Arena is the first thing a new player is
            // shown, and demanding a sign-in before they have seen a round is how you
            // lose them. The uid is real, it scores, and it can be upgraded later.
            runCatching { FirebaseAuth.getInstance().signInAnonymously().await() }
                .onSuccess { result -> result.user?.uid?.let { seedGuestProfile(it) } }
                .onFailure {
                    // Not swallowed. The Arena reads nothing that is not behind
                    // `request.auth != null`, so a failed sign-in does not degrade the
                    // screen — it breaks every query on it, and silence makes that arrive
                    // as a lobby stuck on "Connecting" and a log full of
                    // permission-denied, pointing at the rules rather than at the
                    // provider that is actually switched off.
                    Log.w(TAG, "anonymous sign-in failed: ${it.message}")
                }
        }

        // No session, no reads. Carrying on would start the tick, and the tick retries the
        // round query — several denied reads a minute, none of which can ever succeed.
        if (AuthService.uid == null) {
            _needsSignIn.value = true
            return
        }
        _needsSignIn.value = false

        ArenaClock.sync()
        if (_round.value == null) loadCurrentRound()
        startTicking()
    }

    /**
     * Gives a fresh guest a name, a country and the default avatar — iOS
     * `seedGuestProfile`. The boards take the name from `users/{uid}`, so without this a
     * guest plays under a blank row.
     *
     * `usernameLower` stays empty: a guest has claimed no handle, and reserving one for a
     * uid that dies with the app would retire the name for good. Best-effort — the name is
     * derived from the uid, so any screen can recompute it.
     */
    private suspend fun seedGuestProfile(uid: String) {
        runCatching {
            FirebaseFirestore.getInstance().collection("users").document(uid).set(
                mapOf(
                    "name" to ArenaProfile.guestName(uid),
                    "usernameLower" to "",
                    "country" to java.util.Locale.getDefault().country,
                    "avatar" to AliasAvatar.DEFAULT.id,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }.onFailure { Log.w(TAG, "guest profile seed failed: ${it.message}") }
    }

    /** Stops the clock. The rounds carry on without us. */
    fun leave() {
        ticker?.cancel()
        ticker = null
        // A listener left running would keep reading boards for rounds nobody is looking
        // at, for as long as the app is alive.
        boardListener?.remove()
        boardListener = null
        boardListenerKey = null
        _phase.value = ArenaPhase.Idle
    }

    // MARK: Submitting

    /**
     * Sends what the player actually did — words and times — and never a score.
     *
     * The server scores; the client reports. That is the only arrangement in which a
     * leaderboard means anything, since anything the client computes it can also invent.
     *
     * Fails quietly. A submission that misses the grace window is gone, and there is
     * nothing useful to tell the player at that point — the board they are about to see
     * is the answer.
     *
     * @param roundId the round that was played, not whichever is current. The two differ
     *   after the app is backgrounded mid-round: the engine's timer stops with it and
     *   reports minutes later, by which time the Arena has moved on.
     */
    suspend fun submit(roundId: String, guessed: List<Pair<String, Int>>, skipped: List<String>) {
        val current = _round.value ?: return
        if (current.id != roundId || roundId in submittedRoundIds) return
        submittedRoundIds += roundId

        val payload = mapOf(
            "roundId" to roundId,
            "mode" to _mode.value.raw,
            "guessed" to guessed.map { mapOf("w" to it.first, "ms" to it.second) },
            "skipped" to skipped,
        )

        val sentAt = ArenaClock.nowMs()
        Log.d(
            TAG,
            "submitting $roundId ${_mode.value.raw}: ${guessed.size} guessed, " +
                "${skipped.size} skipped, ${(sentAt - current.endsAtMs) / 1000.0}s after the " +
                "round ended, ${(current.closesAtMs - sentAt) / 1000.0}s before it closes",
        )

        // Sent twice at most. A round is played once and a dropped connection on the last
        // second should not cost it, but a rejection is usually the server saying no
        // rather than the network failing, and repeating it would not change that.
        if (send(payload, roundId)) return

        if (ArenaClock.nowMs() + 1_000 >= current.closesAtMs) {
            submittedRoundIds -= roundId
            return
        }
        delay(1_000)
        if (!send(payload, roundId)) submittedRoundIds -= roundId
    }

    /** One call. True when the server took it. */
    private suspend fun send(payload: Map<String, Any>, roundId: String): Boolean =
        runCatching {
            functions.getHttpsCallable("submitArenaRound").call(payload).await()
            Log.d(TAG, "submitted $roundId")
            true
        }.getOrElse {
            Log.w(TAG, "submit of $roundId failed: ${it.message}")
            false
        }

    // MARK: The tick

    /**
     * Four times a second.
     *
     * A second is enough to draw a countdown, but not to catch the moment a round turns
     * over: on a one-second tick the switch landed anywhere in the second after it, and
     * that lag was visible as the lobby still saying "round in progress". The work per
     * tick is arithmetic against a cached clock offset.
     */
    private fun startTicking() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                tick()
                delay(250)
            }
        }
    }

    private suspend fun tick() {
        ArenaClock.syncIfNeeded()
        val now = ArenaClock.nowMs()

        val round = _round.value
        if (round == null) {
            if (now - lastLookupMs > 5_000) loadCurrentRound()
            return
        }

        if (now >= round.nextStartsAtMs) {
            advanceToNextRound(round)
            return
        }

        _phase.value = phaseOf(round, now)
        _playDeadlineMs.value = System.currentTimeMillis() + (round.endsAtMs - now)

        // In the tail of the break, fetch the round that is coming and its words, so the
        // handover is a local swap rather than two network reads.
        if (_phase.value is ArenaPhase.Results &&
            round.nextStartsAtMs - now <= DEAL_LEAD_MS + 2_000
        ) {
            prefetchNextRound(round)
        }

        // Have the hand before the round opens, not a network hop after it.
        //
        // The rule on `deal/words` opens the read DEAL_LEAD_MS before the start — not at
        // the start, which is what used to make every player lose a round trip off the top
        // of their own round while the lobby said "round in progress".
        if (round.words.isEmpty() && dealingRoundId != round.id) {
            val phase = _phase.value
            when {
                phase is ArenaPhase.Playing &&
                    (round.endsAtMs - now) / 1000 >= FORCE_JOIN_MIN_SEC -> {
                    // Only if the round can still be entered at all. Somebody opening the
                    // Arena with fifteen seconds left is not getting in either way, and
                    // reading thirty words and their clues to tell them so is a wasted trip.
                    dealWords(round)
                }
                phase is ArenaPhase.Lobby && round.startsAtMs - now <= DEAL_LEAD_MS -> {
                    viewModelScope.launch { dealWords(round) }
                }
            }
        }

        val remaining = ((round.endsAtMs - now) / 1000).toInt()
        val hasWords = _round.value?.words?.isNotEmpty() == true
        _canJoinNow.value = _phase.value is ArenaPhase.Playing &&
            remaining >= round.lateJoinMinRemainingSec && hasWords

        _forceJoinSeconds.value =
            if (_phase.value is ArenaPhase.Playing && !_canJoinNow.value &&
                hasWords && remaining >= FORCE_JOIN_MIN_SEC
            ) remaining else null

        estimateLobby()

        if (_phase.value is ArenaPhase.Results) {
            _secondsToResults.value =
                maxOf(0, ((round.boardFetchAtMs - now + 999) / 1000).toInt())
            listenForBoard(round)
            // Nothing is coming: a round nobody submitted to is never scored, so no board
            // is ever written for it.
            if (!_boardLoaded.value && now > round.boardFetchAtMs + BOARD_GIVE_UP_MS) {
                _boardLoaded.value = true
            }
        }
    }

    /**
     * The lobby number.
     *
     * There is no presence collection — nobody writes "I am here", and adding that would
     * mean a write per player per round for a decoration. So the crowd shown is the bots
     * the round has already committed to plus the humans who turned up last time, which is
     * close because the same people play consecutive rounds of a room that loops every
     * couple of minutes.
     *
     * Halved, because `plannedBots` is the round's whole crowd and the bots are dealt
     * evenly across both modes — but a player only ever sees the board of the mode they
     * are in. Promising the full number in the lobby and showing half of it on the board
     * is how the count reads as a lie.
     */
    private fun estimateLobby() {
        val round = _round.value ?: return
        _playersInLobby.value =
            round.plannedBots / ArenaMode.entries.size + maxOf(1, lastHumanCount)
    }

    // MARK: Words

    /**
     * Fetches the round's words. Tried once per round; a failure leaves `words` empty,
     * which keeps [canJoinNow] shut instead of dealing a player an empty round.
     */
    private suspend fun dealWords(round: ArenaRound) {
        if (dealingRoundId == round.id) return
        dealingRoundId = round.id

        val words = fetchDeal(round)
        if (words.isEmpty()) return
        val current = _round.value ?: return
        if (current.id != round.id) return
        _round.value = current.copy(words = words)
    }

    /**
     * Reads a round's words, waiting until the rule will accept the read.
     *
     * Usually there is no wait at all — both callers ask inside the window. The delay is
     * there so asking a moment early costs a pause rather than a refusal.
     */
    private suspend fun fetchDeal(round: ArenaRound): List<ArenaWord> {
        val untilAllowed = (round.startsAtMs - DEAL_LEAD_MS) - ArenaClock.nowMs()
        if (untilAllowed > 0) delay(untilAllowed)

        val doc = runCatching {
            db.collection("arenaRounds").document(round.id)
                .collection("deal").document("words").get().await()
        }.getOrNull() ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val raw = doc.get("words") as? List<Map<String, Any?>> ?: return emptyList()
        return raw.mapNotNull { w ->
            val word = w["w"] as? String ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            ArenaWord(word, (w["clues"] as? List<String>) ?: emptyList())
        }
    }

    // MARK: Loading rounds

    /**
     * Finds the round that is happening now — or, if the scheduler has run ahead of the
     * clock, the one that is about to.
     */
    private suspend fun loadCurrentRound() {
        val now = ArenaClock.nowMs()
        lastLookupMs = now

        // The newest round that has already started. Normally this is the live one; right
        // after a language's very first round is scheduled there is none, and the ordering
        // flips to find the upcoming one instead.
        val started = firstRound {
            it.whereLessThanOrEqualTo("startsAtMs", now)
                .orderBy("startsAtMs", Query.Direction.DESCENDING)
        }
        if (started != null && now < started.nextStartsAtMs) {
            adopt(started)
            return
        }

        firstRound {
            it.whereGreaterThan("startsAtMs", now).orderBy("startsAtMs")
        }?.let { adopt(it) }
    }

    private suspend fun firstRound(shape: (Query) -> Query): ArenaRound? {
        val base = db.collection("arenaRounds").whereEqualTo("lang", _lang.value)
        return runCatching {
            val snap = shape(base).limit(1).get().await()
            snap.documents.firstOrNull()?.let { ArenaRound.from(it.id, it.data) }
        }.getOrNull()
    }

    /**
     * Loads the round after [previous], and its words, without disturbing what is on
     * screen. Runs once per round — [pendingRoundId] is set before the read, not after.
     */
    private suspend fun prefetchNextRound(previous: ArenaRound) {
        val nextId = "${previous.lang}_${previous.index + 1}"
        if (pendingRoundId == nextId) return
        pendingRoundId = nextId

        val doc = runCatching {
            db.collection("arenaRounds").document(nextId).get().await()
        }.getOrNull() ?: return
        val next = ArenaRound.from(doc.id, doc.data) ?: return
        pendingRound = next.copy(words = fetchDeal(next))
    }

    private suspend fun advanceToNextRound(previous: ArenaRound) {
        if (advancing) return
        advancing = true
        try {
            val nextId = "${previous.lang}_${previous.index + 1}"

            // Already in hand, words and all.
            val pending = pendingRound
            if (pending != null && pending.id == nextId) {
                pendingRound = null
                pendingRoundId = null
                adopt(pending)
                return
            }

            val doc = runCatching {
                db.collection("arenaRounds").document(nextId).get().await()
            }.getOrNull()
            val next = doc?.let { ArenaRound.from(it.id, it.data) }
            if (next != null) {
                adopt(next)
            } else {
                _round.value = null
                loadCurrentRound()
            }
        } finally {
            advancing = false
        }
    }

    private fun adopt(next: ArenaRound) {
        _round.value = next
        dealingRoundId = null
        resetBoard()
        _phase.value = phaseOf(next, ArenaClock.nowMs())
        estimateLobby()
    }

    // MARK: The board

    private fun resetBoard() {
        _board.value = emptyList()
        _boardLoaded.value = false
        boardListener?.remove()
        boardListener = null
        boardListenerKey = null
    }

    /**
     * Watches this round's board instead of guessing when it will appear.
     *
     * It used to be two timed reads — one six seconds after submissions closed, one two
     * seconds later — and then whatever came back. That held for rounds nobody played,
     * which are skipped and scored in a single write, and broke on the first round that
     * had anything in it.
     */
    private fun listenForBoard(round: ArenaRound) {
        val key = "${round.id}/${_mode.value.raw}"
        if (boardListenerKey == key) return

        boardListener?.remove()
        boardListenerKey = key
        val me = AuthService.uid

        boardListener = db.collection("arenaRounds").document(round.id)
            .collection("board").document(_mode.value.raw)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                if (boardListenerKey != key) return@addSnapshotListener

                @Suppress("UNCHECKED_CAST")
                val raw = snap.get("entries") as? List<Map<String, Any?>> ?: emptyList()
                _board.value = raw.mapNotNull { ArenaBoardEntry.from(it, me) }
                _boardLoaded.value = true
                if (raw.isNotEmpty()) {
                    _playersNow.value = (snap.get("players") as? Number)?.toInt() ?: raw.size
                    lastHumanCount = raw.count { it["isBot"] != true }
                }
            }
    }

    // MARK: Language and mode preferences

    fun setMode(next: ArenaMode) {
        if (next == _mode.value) return
        _mode.value = next
        prefs.edit().putString(KEY_MODE, next.raw).apply()
        resetBoard()
    }

    fun setLanguage(code: String) {
        if (code !in LANGUAGES || code == _lang.value) return

        _lang.value = code
        prefs.edit().putString(KEY_LANG, code).apply()
        _round.value = null
        _phase.value = ArenaPhase.Idle
        _playDeadlineMs.value = null
        _canJoinNow.value = false
        dealingRoundId = null
        // A round prefetched for the language being left would otherwise be handed over
        // as if it belonged to the new one.
        pendingRound = null
        pendingRoundId = null
        lastLookupMs = 0
        resetBoard()

        viewModelScope.launch { loadCurrentRound() }
    }

    private fun savedMode(): ArenaMode =
        ArenaMode.from(appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, null))

    /**
     * What the Arena opens on: the language last chosen here, or — the first time —
     * whatever the app is already set to, when that is a language the Arena runs.
     */
    private fun preferredLanguage(): String {
        val saved = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
        if (saved != null && saved in LANGUAGES) return saved
        val appLang = ArenaLanguages.fromAppLocale(appCtx)
        return if (appLang in LANGUAGES) appLang else "eng"
    }
}

/** Maps the app's locale onto the Arena's deck languages. */
object ArenaLanguages {
    fun fromAppLocale(context: Context): String =
        when (context.resources.configuration.locales[0].language.lowercase()) {
            "hr" -> "cro"
            "sr" -> "srb"
            "bs" -> "cro"   // no Bosnian deck: no clue file, so no deck is written
            "sl" -> "slo"
            "de" -> "ger"
            "es" -> "esp"
            "it" -> "ita"
            "fr" -> "fra"
            else -> "eng"
        }
}
