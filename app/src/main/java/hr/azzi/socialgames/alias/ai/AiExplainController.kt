package hr.azzi.socialgames.alias.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import hr.azzi.socialgames.alias.Service.SoundSystem
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
    enum class Phase { Loading, Denied, Thinking, Guessing, Done }

    private val sound = SoundSystem(context)
    private val speaker = AISpeaker(context)
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
    var transcript by mutableStateOf(""); private set
    var remaining by mutableDoubleStateOf(config.totalSeconds.toDouble()); private set
    var flash by mutableStateOf(false); private set
    val correctWords: SnapshotStateList<String> = mutableStateListOf()
    val skippedWords: SnapshotStateList<String> = mutableStateListOf()

    private var index by mutableIntStateOf(0)
    private var clueRotation: List<String> = emptyList()
    private var clueStep = 0
    private var advancing = false
    private var lastSkipAt = 0L
    private var clueJob: Job? = null
    private var evalJob: Job? = null

    private val firstClueGap = 3_000L
    private val nextClueGap = 5_000L
    private val evalDebounce = 1_200L

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
        speaker.onSpeakingChanged = { speaking -> if (!speaking) clueSpoken() }
        speech.locale = config.language.locale
        speech.onTranscript = { t -> transcript = t; scheduleEval() }
        beginWord()
        startTimer()
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
        onFinish?.invoke(result, frozenWords)
    }
}
