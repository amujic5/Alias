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
    // The engine's OnInitListener isn't guaranteed to run on the caller's thread,
    // so the init state is volatile and the waiter list is guarded — otherwise a
    // waiter added just as init completes can be dropped and never called back.
    @Volatile private var ready = false
    @Volatile private var initialized = false
    private val initWaiters = mutableListOf<() -> Unit>()
    private var pending: Pair<String, String>? = null
    var onSpeakingChanged: ((Boolean) -> Unit)? = null
    /** Fired (on main) when none of the accepted voices has data installed, so the
     *  UI can offer to download one. Carries every locale that would do. */
    var onVoiceUnavailable: ((List<Locale>) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        initialized = true
        if (ready) { tts.setSpeechRate(rate); pending?.let { (t, l) -> pending = null; speak(t, l) } }
        val waiters = synchronized(initWaiters) { initWaiters.toList().also { initWaiters.clear() } }
        main.post { waiters.forEach { it() } }
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
     * initialized. [onResult] gets an empty list if it's good to go, otherwise
     * every locale whose voice would do — installing any one of them is enough.
     * Lets callers gate a round on a usable voice instead of discovering the
     * silence only after starting.
     */
    fun checkVoice(code: String, onResult: (List<Locale>) -> Unit) {
        runWhenInitialized { onResult(if (ready) resolveVoice(code) else voiceCandidates(code)) }
    }

    private fun runWhenInitialized(block: () -> Unit) {
        val run = synchronized(initWaiters) {
            if (initialized) true else { initWaiters += block; false }
        }
        if (run) block()
    }

    /**
     * Select the first installed voice among [voiceCandidates]. Returns an empty
     * list once one is in use, otherwise all the candidates — the caller offers
     * them as a choice, since installing any single one makes the round playable.
     */
    private fun resolveVoice(code: String): List<Locale> {
        val candidates = voiceCandidates(code)
        for (loc in candidates) {
            if (usable(tts.setLanguage(loc))) return emptyList()
        }
        return candidates
    }

    private fun applyLanguage(code: String) {
        val needs = resolveVoice(code)
        if (needs.isNotEmpty()) main.post { onVoiceUnavailable?.invoke(needs) }
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

    companion object {
        /**
         * Voices that would satisfy [code], best first. Android's engine ships
         * Croatian but almost never Serbian or Bosnian, so those also accept a
         * Croatian voice — shared Latin pronunciation, and hr ijekavica is closer
         * to Bosnian/Montenegrin than sr ekavica anyway. The player's own language
         * is always tried first, so a device that does have it still uses it.
         */
        fun voiceCandidates(code: String): List<Locale> {
            val primary = localeFor(code)
            return when (code.substringBefore("-").lowercase()) {
                "sr", "bs" -> listOf(primary, Locale("hr", "HR"))
                else -> listOf(primary)
            }
        }

        private fun localeFor(code: String): Locale {
            val parts = code.split("-")
            return if (parts.size == 2) Locale(parts[0], parts[1]) else Locale(parts[0])
        }
    }
}
