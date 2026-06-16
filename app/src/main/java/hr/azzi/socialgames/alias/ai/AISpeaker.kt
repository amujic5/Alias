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
    private var pending: Pair<String, String>? = null
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) { tts.setSpeechRate(rate); pending?.let { (t, l) -> pending = null; speak(t, l) } }
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
        tts.language = localeFor(locale)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "ai-clue")
    }

    fun stop() {
        tts.stop()
        main.post { onSpeakingChanged?.invoke(false) }
    }

    fun shutdown() {
        runCatching { tts.stop(); tts.shutdown() }
    }

    private fun localeFor(code: String): Locale {
        val parts = code.split("-")
        return if (parts.size == 2) Locale(parts[0], parts[1]) else Locale(parts[0])
    }
}
