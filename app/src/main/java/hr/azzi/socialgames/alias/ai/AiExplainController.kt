package hr.azzi.socialgames.alias.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import hr.azzi.socialgames.alias.AppReview
import hr.azzi.socialgames.alias.Service.SoundSystem
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * "AI explains → you guess" game logic. Port of iOS AIExplainView.
 * AI reads a hidden word's clue (TTS); player taps the mic and says the guess
 * (speech recognition); fuzzy-matched against the target.
 */
class AiExplainController(
    private val context: Context,
    val config: AIPracticeConfig,
    fixedWords: List<String>?,
    private val onFinish: ((AIPracticeResult, List<String>) -> Unit)?,
) {
    enum class Phase { Loading, VoiceRequired, Denied, Thinking, Guessing, Done }

    private val sound = SoundSystem(context)
    private val speaker = AISpeaker(context, rate = 1.2f)
    private val speech = AISpeech(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val frozenWords: List<String> =
        fixedWords ?: AIDeckCatalog.words(context, config.deck.id, config.language).shuffled()
    private var words: MutableList<String> = frozenWords.toMutableList()
    private val baked: Map<String, List<String>> =
        AIDeckCatalog.clueMap(context, config.deck.id, config.language)

    var phase by mutableStateOf(Phase.Loading); private set
    var clue by mutableStateOf(""); private set
    var listening by mutableStateOf(false); private set
    /** True while the AI is speaking a clue (TTS active) — drives the bot avatar's
     *  "talking" animation. */
    var speaking by mutableStateOf(false); private set
    var transcript by mutableStateOf(""); private set
    var remaining by mutableDoubleStateOf(config.totalSeconds.toDouble()); private set
    var flash by mutableStateOf(false); private set
    /** Non-empty while play is blocked because no accepted AI voice is installed;
     *  carries every locale that would unblock it. */
    var voiceMissing by mutableStateOf<List<Locale>>(emptyList()); private set
    private var voiceChecking = false
    /** Bumped per gate run so a stale engine callback can't touch a newer check. */
    private var voiceGate = 0
    private var voiceWatchdog: Job? = null
    private var sttLogged = false
    val correctWords: SnapshotStateList<String> = mutableStateListOf()
    val skippedWords: SnapshotStateList<String> = mutableStateListOf()

    private var index by mutableIntStateOf(0)
    private var clueRotation: List<String> = emptyList()
    private var clueStep = 0
    private var advancing = false
    private var lastSkipAt = 0L
    private var clueJob: Job? = null
    private var evalJob: Job? = null

    private val firstClueGap = 5_000L
    private val nextClueGap = 5_000L
    private val evalDebounce = 1_200L
    /** How long to wait on the TTS engine before offering the download prompt.
     *  Generous on purpose: a cold engine can take several seconds to bind. */
    private val VOICE_PROMPT_DELAY_MS = 8_000L

    private val word: String get() = words.getOrElse(index) { "" }

    val result: AIPracticeResult
        get() = AIPracticeResult(
            deckName = deckDisplayName(),
            language = config.language.aiName,
            correctWords = correctWords.toList(),
            skippedWords = skippedWords.toList(),
            totalSeconds = config.totalSeconds,
        )

    private fun deckDisplayName(): String {
        val id = context.resources.getIdentifier(config.deck.nameRes, "string", context.packageName)
        return if (id != 0) context.getString(id) else config.deck.id
    }

    /** Call after RECORD_AUDIO is granted. */
    fun start() {
        speaker.onSpeakingChanged = { s -> speaking = s; if (!s) clueSpoken() }
        speech.locale = config.language.locale
        speech.onTranscript = { t -> transcript = t; logSttSuccessOnce(t); scheduleEval() }
        // Mic couldn't be sustained: reflect that the app stopped listening.
        speech.onFailed = { listening = false }
        speech.onLanguageUnavailable = {
            SpeechAnalytics.sttResult(context, config.language, config.deck.id, success = false)
        }
        AIInterstitial.preload(context)
        // The AI voice is a hard requirement — never start the round without it.
        runVoiceGate()
    }

    /**
     * Block on a usable TTS voice; only start the round once it's confirmed.
     * Re-runs every time — no caching.
     *
     * [reloadEngine] reconnects to the TTS service first; pass it when coming back
     * from the voice installer, because the engine this instance is bound to won't
     * report a voice that was installed after the connection was made. It also
     * overrides the in-flight guard: this is a deliberate fresh check, and the gate
     * counter drops whatever the previous one was still waiting on.
     */
    fun runVoiceGate(reloadEngine: Boolean = false) {
        if (voiceChecking && !reloadEngine) return
        voiceChecking = true
        phase = Phase.Loading
        val gate = ++voiceGate
        voiceWatchdog?.cancel()
        if (reloadEngine) speaker.reload()

        // The engine's answer is the only authority, and it is allowed to arrive
        // late: binding to a cold TTS service can take many seconds, so it may well
        // land after the prompt below has already been shown, and it then overrules
        // it either way.
        speaker.checkVoice(config.language.locale) { needs ->
            if (gate != voiceGate) return@checkVoice
            voiceChecking = false
            voiceWatchdog?.cancel()
            SpeechAnalytics.voiceResult(context, config.language, config.deck.id, success = needs.isEmpty())
            if (needs.isNotEmpty()) {
                voiceMissing = needs; phase = Phase.VoiceRequired
            } else if (phase == Phase.Loading || phase == Phase.VoiceRequired) {
                voiceMissing = emptyList(); beginWord(); startTimer()
            }
        }

        // Nothing back yet: show the prompt so the player isn't left on a spinner,
        // but do NOT treat it as a verdict. This used to settle the gate, so a slow
        // first init was reported as a missing voice — which is why leaving the
        // screen and coming back (warm service, fast init) appeared to fix it.
        voiceWatchdog = scope.launch {
            delay(VOICE_PROMPT_DELAY_MS)
            if (gate == voiceGate && voiceChecking) {
                voiceMissing = AISpeaker.voiceCandidates(config.language.locale)
                phase = Phase.VoiceRequired
            }
        }
    }

    /** Log STT success once per session, on the first transcript that comes back. */
    private fun logSttSuccessOnce(text: String) {
        if (sttLogged || text.isBlank()) return
        sttLogged = true
        SpeechAnalytics.sttResult(context, config.language, config.deck.id, success = true)
    }

    fun denied() { phase = Phase.Denied }

    fun restart() {
        words = AIDeckCatalog.words(context, config.deck.id, config.language).shuffled().toMutableList()
        index = 0
        remaining = config.totalSeconds.toDouble()
        correctWords.clear(); skippedWords.clear()
        beginWord()
        startTimer()
    }

    fun dispose() {
        clueJob?.cancel(); evalJob?.cancel()
        speaker.shutdown(); speech.destroy()
        scope.cancel()
    }

    private fun startTimer() {
        scope.launch {
            var tickPlayed = false
            while (isActive) {
                delay(100)
                if (phase != Phase.Guessing && phase != Phase.Thinking) continue
                remaining -= 0.1
                if (remaining <= 10 && !tickPlayed) { tickPlayed = true; sound.playTikTok() }
                if (remaining <= 0) { finish(); break }
            }
        }
    }

    private fun beginWord() {
        clue = ""; listening = false; clueStep = 0; advancing = false; transcript = ""
        clueJob?.cancel(); evalJob?.cancel()
        val b = baked[word.lowercase()] ?: emptyList()
        clueRotation = if (b.size > 1) {
            val startIdx = (b.indices).random()
            (b.indices).map { b[(startIdx + it) % b.size] }
        } else b
        phase = Phase.Thinking
        speaker.stop(); speech.stop()
        showStep(0)
    }

    private fun showStep(step: Int) {
        val target = word
        val text = clueRotation.getOrNull(step)
        if (text.isNullOrEmpty()) {
            if (clue.isEmpty()) clue = "…"
            phase = Phase.Guessing
            return
        }
        if (word != target) return
        clue = text
        phase = Phase.Guessing
        listening = false
        speaker.stop(); speech.stop()
        speaker.speak(text, config.language.locale)
    }

    fun answerNow() {
        clueJob?.cancel()
        listening = true
        transcript = ""
        speaker.stop()
        speech.start()
    }

    private fun clueSpoken() {
        if (phase != Phase.Guessing || listening || clueStep + 1 >= clueRotation.size) return
        val gap = if (clueStep == 0) firstClueGap else nextClueGap
        clueJob?.cancel()
        clueJob = scope.launch { delay(gap); advanceClue() }
    }

    private fun advanceClue() {
        if (phase != Phase.Guessing || advancing || clueStep + 1 >= clueRotation.size) return
        advancing = true
        clueStep += 1
        showStep(clueStep)
        advancing = false
    }

    private fun scheduleEval() {
        if (phase != Phase.Guessing || !listening || word.isEmpty()) return
        if (AnswerMatcher.matches(transcript, listOf(word.lowercase()))) { evalJob?.cancel(); correct(); return }
        val text = transcript.trim()
        if (text.length < 2) return
        evalJob?.cancel()
        evalJob = scope.launch {
            delay(evalDebounce)
            handleWrongGuess(text)
        }
    }

    private fun handleWrongGuess(utterance: String) {
        if (phase != Phase.Guessing || advancing || word.isEmpty()) return
        if (AnswerMatcher.matches(utterance, listOf(word.lowercase()))) { correct(); return }
        clueJob?.cancel()
        if (clueStep + 1 < clueRotation.size) advanceClue()
        else { speaker.stop(); speech.start() }
    }

    private fun correct() {
        correctWords.add(word)
        sound.playRightButton()
        flash = true
        scope.launch { delay(250); flash = false }
        next()
    }

    fun hearAgain() { speaker.speak(clue, config.language.locale) }

    fun newClue() {
        evalJob?.cancel(); clueJob?.cancel()
        if (clueStep + 1 < clueRotation.size) advanceClue()
        else { clueStep = 0; showStep(0) }
    }

    fun reveal() {
        val now = System.currentTimeMillis()
        if (now - lastSkipAt < 1000) return
        lastSkipAt = now
        skippedWords.add(word)
        sound.playSkipButton()
        next()
    }

    private fun next() {
        evalJob?.cancel(); clueJob?.cancel()
        speaker.stop(); speech.stop()
        if (index + 1 < words.size) index += 1
        else if (onFinish == null) { words.shuffle(); index = 0 }
        else index = 0
        beginWord()
    }

    private fun finish() {
        if (phase == Phase.Done) return
        phase = Phase.Done
        clueJob?.cancel(); evalJob?.cancel()
        speaker.stop(); speech.stop()
        sound.playEnd()
        onFinish?.invoke(result, frozenWords)                 // show the score screen first…
        AIInterstitial.show(context) { AppReview.request(context) } // …ad, then the rating prompt
    }
}
