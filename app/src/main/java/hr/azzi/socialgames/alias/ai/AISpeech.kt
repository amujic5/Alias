package hr.azzi.socialgames.alias.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Continuous speech recognition wrapper. Restarts itself while [active] so the
 * player can keep guessing out loud. Emits partial + final transcripts.
 * Must be created/used on the main thread.
 */
class AISpeech(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    var locale: String = "en-US"
    var onTranscript: ((String) -> Unit)? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        active = true
        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
        }
        listen()
    }

    /** Pause listening but keep the recognizer for a quick restart. */
    fun stop() {
        active = false
        runCatching { recognizer?.cancel() }
    }

    fun destroy() {
        active = false
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun listen() {
        if (!active) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer?.startListening(intent) }
    }

    private fun restartSoon() {
        if (!active) return
        main.postDelayed({ if (active) { runCatching { recognizer?.cancel() }; listen() } }, 250)
    }

    private fun emit(results: Bundle?) {
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = list?.firstOrNull() ?: return
        if (text.isNotBlank()) onTranscript?.invoke(text)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) { emit(partialResults) }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onResults(results: Bundle?) { emit(results); restartSoon() }
        override fun onError(error: Int) { restartSoon() }
    }
}
