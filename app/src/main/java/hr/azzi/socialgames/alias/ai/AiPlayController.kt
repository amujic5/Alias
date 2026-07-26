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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

/** "You explain → AI guesses" game logic. Port of iOS AIPlayView. Turn-based:
 *  tap mic to explain (AI silent), tap again to hand over → AI guesses via the
 *  on-device embedding index. Saying the word/root forfeits it. */
class AiPlayController(
    private val context: Context,
    val config: AIPracticeConfig,
    fixedWords: List<String>?,
    private val onFinish: ((AIPracticeResult, List<String>) -> Unit)?,
) {
    enum class Phase { Loading, VoiceRequired, Denied, Playing, Done }
    enum class Turn { Idle, Explaining, AiGuessing }

    private val sound = SoundSystem(context)
    private val speaker = AISpeaker(context, rate = 0.6f)
    private val speech = AISpeech(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val frozenWords: List<String> =
        fixedWords ?: AIDeckCatalog.words(context, config.deck.id, config.language).shuffled()
    private var words: MutableList<String> = frozenWords.toMutableList()

    var phase by mutableStateOf(Phase.Loading); private set
    var turn by mutableStateOf(Turn.Idle); private set
    /** True while the AI is speaking a guess (TTS active) — drives the bot avatar's
     *  "talking" animation. */
    var speaking by mutableStateOf(false); private set
    var aiGuess by mutableStateOf<String?>(null); private set
    var transcript by mutableStateOf(""); private set
    var remaining by mutableDoubleStateOf(config.totalSeconds.toDouble()); private set
    var flash by mutableStateOf(false); private set
    var penaltyFlash by mutableStateOf(false); private set
    /** Non-null while play is blocked because the AI voice for this language has
     *  no data installed; carries the locale to install. */
    var voiceMissing by mutableStateOf<Locale?>(null); private set
    private var voiceChecking = false
    /** Bumped per gate run so a stale engine callback can't touch a newer check. */
    private var voiceGate = 0
    private var voiceWatchdog: Job? = null
    /** How long to wait on the TTS engine before offering the download prompt.
     *  Generous on purpose: a cold engine can take several seconds to bind. */
    private val VOICE_PROMPT_DELAY_MS = 8_000L
    private var sttLogged = false
    val correctWords: SnapshotStateList<String> = mutableStateListOf()
    val skippedWords: SnapshotStateList<String> = mutableStateListOf()

    private var index by mutableIntStateOf(0)
    private val descs = ArrayList<String>()
    private val aiWrong = ArrayList<String>()
    private var resolving = false
    private var thinking = false
    private var lastSkipAt = 0L
    private var indexReady = false

    val word: String get() = words.getOrElse(index) { "" }

    val result: AIPracticeResult
        get() = AIPracticeResult(deckDisplayName(), config.language.aiName,
            correctWords.toList(), skippedWords.toList(), config.totalSeconds)

    private fun deckDisplayName(): String {
        val id = context.resources.getIdentifier(config.deck.nameRes, "string", context.packageName)
        return if (id != 0) context.getString(id) else config.deck.id
    }

    fun start() {
        speaker.onSpeakingChanged = { s -> speaking = s }
        speech.locale = config.language.locale
        speech.onTranscript = { t -> transcript = t; logSttSuccessOnce(t); checkForbidden() }
        // Mic couldn't be sustained: stop pretending to listen, reset the button to Idle.
        speech.onFailed = { if (turn == Turn.Explaining) { turn = Turn.Idle } }
        speech.onLanguageUnavailable = {
            SpeechAnalytics.sttResult(context, config.language, config.deck.id, success = false)
        }
        scope.launch(Dispatchers.Default) {
            indexReady = AIWordIndex.prepare(context, config.deck.id, config.language)
        }
        AIInterstitial.preload(context)
        // The AI voice is a hard requirement — never start the round without it.
        runVoiceGate()
    }

    /** Block on a usable TTS voice; only start the round once it's confirmed.
     *  Re-runs every time (e.g. on returning from the installer) — no caching. */
    fun runVoiceGate() {
        if (voiceChecking) return
        voiceChecking = true
        phase = Phase.Loading
        val gate = ++voiceGate
        voiceWatchdog?.cancel()

        // The engine's answer is the only authority, and it is allowed to arrive
        // late: binding to a cold TTS service can take many seconds, so it may well
        // land after the prompt below has already been shown, and it then overrules
        // it either way.
        speaker.checkVoice(config.language.locale) { needs ->
            if (gate != voiceGate) return@checkVoice
            voiceChecking = false
            voiceWatchdog?.cancel()
            SpeechAnalytics.voiceResult(context, config.language, config.deck.id, success = needs == null)
            if (needs != null) {
                voiceMissing = needs; phase = Phase.VoiceRequired
            } else if (phase == Phase.Loading || phase == Phase.VoiceRequired) {
                voiceMissing = null; beginWord(); startTimer()
            }
        }

        // Nothing back yet: show the prompt so the player isn't left on a spinner,
        // but do NOT treat it as a verdict. This used to settle the gate, so a slow
        // first init was reported as a missing voice — which is why leaving the
        // screen and coming back (warm service, fast init) appeared to fix it.
        voiceWatchdog = scope.launch {
            delay(VOICE_PROMPT_DELAY_MS)
            if (gate == voiceGate && voiceChecking) {
                voiceMissing = config.language.javaLocale
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
        index = 0; remaining = config.totalSeconds.toDouble()
        correctWords.clear(); skippedWords.clear()
        beginWord(); startTimer()
    }

    fun dispose() {
        speaker.shutdown(); speech.destroy(); scope.cancel()
    }

    private fun startTimer() {
        scope.launch {
            var tick = false
            while (isActive) {
                delay(100)
                if (phase != Phase.Playing) continue
                remaining -= 0.1
                if (remaining <= 10 && !tick) { tick = true; sound.playTikTok() }
                if (remaining <= 0) { finish(); break }
            }
        }
    }

    private fun beginWord() {
        aiGuess = null; resolving = false; thinking = false
        aiWrong.clear(); penaltyFlash = false; descs.clear(); transcript = ""
        turn = Turn.Idle
        phase = Phase.Playing
        speech.stop()
    }

    fun toggleExplain() {
        if (phase != Phase.Playing || resolving) return
        when (turn) {
            Turn.Idle -> {
                aiGuess = null; speaker.stop()
                turn = Turn.Explaining
                transcript = ""
                speech.start()
            }
            Turn.Explaining -> {
                val burst = transcript.trim()
                speech.stop()
                if (burst.isNotEmpty()) descs.add(burst)
                turn = Turn.AiGuessing
                scope.launch { runGuess() }
            }
            Turn.AiGuessing -> {}
        }
    }

    private fun checkForbidden() {
        if (phase != Phase.Playing || turn != Turn.Explaining || resolving) return
        if (saidForbidden(transcript, word)) forbiddenSpoken()
    }

    private fun forbiddenSpoken() {
        if (resolving) return
        resolving = true
        speech.stop()
        skippedWords.add(word)
        sound.playSkipButton()
        penaltyFlash = true
        scope.launch { delay(1100); penaltyFlash = false; next() }
    }

    private suspend fun runGuess() {
        if (resolving || thinking) return
        thinking = true
        try {
            val queries = ArrayList<String>()
            val joined = descs.joinToString(" ").trim()
            if (joined.length >= 2) queries.add(joined)
            for (d in descs) { val t = d.trim(); if (t.length >= 2 && !queries.contains(t)) queries.add(t) }
            val deck = AIWordIndex.deck(config.deck.id, config.language)
            if (queries.isEmpty() || deck == null) { turn = Turn.Idle; return }

            val shortlists = withContext(Dispatchers.Default) {
                queries.mapNotNull { q ->
                    AIEmbedder.embed(q)?.let { qv -> AIWordIndex.topK(qv, deck, 12) }
                }
            }
            val hit = shortlists.any { list -> list.any { AnswerMatcher.matches(it, listOf(word)) } }
            if (hit) { correct(); return }
            bestGuess(shortlists)?.let { g ->
                if (!aiWrong.contains(g)) aiWrong.add(g)
                aiGuess = g
                speaker.speak(g, config.language.locale)
            }
            turn = Turn.Idle
        } finally {
            thinking = false
        }
    }

    private fun bestGuess(shortlists: List<List<String>>): String? {
        val score = HashMap<String, Double>()
        for (list in shortlists) {
            list.take(5).forEachIndexed { i, c -> score[c] = (score[c] ?: 0.0) + 1.0 / (i + 1) }
        }
        val ranked = score.entries.sortedByDescending { it.value }.map { it.key }
        return ranked.firstOrNull { !aiWrong.contains(it) } ?: ranked.firstOrNull()
    }

    private fun correct() {
        if (resolving) return
        resolving = true
        val guessed = word
        speaker.speak(guessed, config.language.locale)
        scope.launch {
            delay(800)
            if (phase != Phase.Playing) return@launch
            correctWords.add(guessed)
            sound.playRightButton()
            flash = true
            launch { delay(250); flash = false }
            next()
        }
    }

    fun skip() {
        val now = System.currentTimeMillis()
        if (now - lastSkipAt < 1000) return
        lastSkipAt = now
        skippedWords.add(word)
        sound.playSkipButton()
        next()
    }

    private fun next() {
        speech.stop()
        if (index + 1 < words.size) index += 1
        else if (onFinish == null) { words.shuffle(); index = 0 }
        else index = 0
        beginWord()
    }

    private fun finish() {
        if (phase == Phase.Done) return
        phase = Phase.Done
        speaker.stop(); speech.stop()
        sound.playEnd()
        onFinish?.invoke(result, frozenWords)                 // show the score screen first…
        AIInterstitial.show(context) { AppReview.request(context) } // …ad, then the rating prompt
    }

    /** True if any spoken token equals the target word/token or shares its root. */
    private fun saidForbidden(transcript: String, word: String): Boolean {
        fun tokens(s: String): List<String> =
            Normalizer.normalize(AnswerMatcher.cyrillicToLatin(s), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "").lowercase()
                .split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        val spoken = tokens(transcript)
        val targets = tokens(word).filter { it.length >= 3 }
        if (spoken.isEmpty() || targets.isEmpty()) return false
        for (t in targets) {
            val root = t.take(maxOf(4, t.length - 3))
            for (w in spoken) {
                if (w == t) return true
                if (root.length >= 4 && w.length >= 4 && (w.startsWith(root) || root.startsWith(w))) return true
            }
        }
        return false
    }
}
