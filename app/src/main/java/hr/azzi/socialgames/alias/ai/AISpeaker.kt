package hr.azzi.socialgames.alias.ai

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Text-to-speech wrapper that speaks AI clues. Port of iOS AISpeaker. */
class AISpeaker(context: Context, private val rate: Float = 1.0f) {

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var initialized = false
    private val initWaiters = mutableListOf<() -> Unit>()
    private var pending: Pair<String, String>? = null
    var onSpeakingChanged: ((Boolean) -> Unit)? = null
    /** Fired (on main) when the requested voice — and its fallback — has no data
     *  installed, so the UI can offer to download it. Carries the locale that
     *  would actually need installing. */
    var onVoiceUnavailable: ((Locale) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        initialized = true
        if (ready) { tts.setSpeechRate(rate); pending?.let { (t, l) -> pending = null; speak(t, l) } }
        val waiters = initWaiters.toList(); initWaiters.clear()
        waiters.forEach { it() }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { main.post { onSpeakingChanged?.invoke(true) } }
            override fun onDone(utteranceId: String?) { main.post { onSpeakingChanged?.invoke(false) } }
            @Deprecated("deprecated") override fun onError(utteranceId: String?) { main.post { onSpeakingChanged?.invoke(false) } }
        })
    }

    fun speak(text: String, locale: String) {
        if (text.isBlank()) return
        if (!ready) { pending = text to locale; return }
        applyLanguage(locale)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "ai-clue")
    }

    fun stop() {
        tts.stop()
        main.post { onSpeakingChanged?.invoke(false) }
    }

    fun shutdown() {
        runCatching { tts.stop(); tts.shutdown() }
    }

    /**
     * Report whether the voice for [code] can speak, once the engine has
     * initialized. [onResult] gets null if it's good to go, or the locale that
     * needs its data installed. Lets callers gate a round on a usable voice
     * instead of discovering the silence only after starting.
     */
    fun checkVoice(code: String, onResult: (Locale?) -> Unit) {
        runWhenInitialized { onResult(if (ready) resolveVoice(code) else localeFor(code)) }
    }

    private fun runWhenInitialized(block: () -> Unit) {
        if (initialized) block() else initWaiters += block
    }

    /**
     * Select the voice for [code]; if that voice isn't installed, fall back to a
     * mutually-intelligible one. Google's on-device engine ships Croatian but
     * almost never Serbian, so sr → hr keeps AI clues audible (hr ijekavica is
     * also closer to Montenegrin pronunciation than sr ekavica). Returns null if
     * a usable voice was selected, otherwise the locale that needs installing.
     */
    private fun resolveVoice(code: String): Locale? {
        var effective = localeFor(code)
        var res = tts.setLanguage(effective)
        if (!usable(res)) {
            fallbackLocale(code)?.let { fb -> effective = fb; res = tts.setLanguage(fb) }
        }
        return if (usable(res)) null else effective
    }

    private fun applyLanguage(code: String) {
        resolveVoice(code)?.let { loc -> main.post { onVoiceUnavailable?.invoke(loc) } }
    }

    /**
     * A voice is only usable if [res] is non-negative AND the selected voice's
     * data is actually present. Google's engine reports LANG_COUNTRY_AVAILABLE
     * for downloadable-but-not-yet-installed voices, so we also reject any voice
     * flagged NOT_INSTALLED — otherwise speak() succeeds silently.
     */
    private fun usable(res: Int): Boolean {
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) return false
        val features = runCatching { tts.voice?.features }.getOrNull() ?: return true
        return TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features
    }

    private fun fallbackLocale(code: String): Locale? =
        when (code.substringBefore("-").lowercase()) {
            "sr" -> Locale("hr", "HR")
            else -> null
        }

    private fun localeFor(code: String): Locale {
        val parts = code.split("-")
        return if (parts.size == 2) Locale(parts[0], parts[1]) else Locale(parts[0])
    }
}
