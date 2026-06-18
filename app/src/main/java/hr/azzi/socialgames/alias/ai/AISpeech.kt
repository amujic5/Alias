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
 *
 * Robustness: a single scheduled restart at a time (no colliding cancel/start
 * races), normal session ends just re-listen, "stuck" errors (BUSY/CLIENT/SERVER)
 * destroy+recreate the recognizer, and after several hard failures in a row it
 * gives up and calls [onFailed] so the UI can reset (instead of silently looking
 * like it's still listening). Must be created/used on the main thread.
 */
class AISpeech(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var pendingRestart: Runnable? = null
    private var failures = 0

    var locale: String = "en-US"
    var onTranscript: ((String) -> Unit)? = null
    /** Called when listening could not be sustained (mic unavailable / repeated errors). */
    var onFailed: (() -> Unit)? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    private companion object {
        const val MAX_HARD_FAILURES = 4
        const val RESTART_DELAY_MS = 300L
    }

    fun start() {
        active = true
        failures = 0
        cancelPendingRestart()
        ensureRecognizer()
        listen()
    }

    /** Pause listening but keep the recognizer for a quick restart. */
    fun stop() {
        active = false
        cancelPendingRestart()
        runCatching { recognizer?.cancel() }
    }

    fun destroy() {
        active = false
        cancelPendingRestart()
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun ensureRecognizer() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(listener)
            }
        }
    }

    private fun listen() {
        if (!active) return
        ensureRecognizer()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer?.startListening(intent) }
    }

    /** Schedule exactly one restart; recreate the recognizer first if it got stuck. */
    private fun restartSoon(recreate: Boolean) {
        if (!active || pendingRestart != null) return
        val r = Runnable {
            pendingRestart = null
            if (!active) return@Runnable
            if (recreate) {
                runCatching { recognizer?.destroy() }
                recognizer = null
                ensureRecognizer()
            }
            listen()
        }
        pendingRestart = r
        main.postDelayed(r, RESTART_DELAY_MS)
    }

    private fun cancelPendingRestart() {
        pendingRestart?.let { main.removeCallbacks(it) }
        pendingRestart = null
    }

    private fun fail() {
        active = false
        cancelPendingRestart()
        runCatching { recognizer?.cancel() }
        onFailed?.invoke()
    }

    private fun emit(results: Bundle?) {
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = list?.firstOrNull() ?: return
        if (text.isNotBlank()) onTranscript?.invoke(text)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { failures = 0 }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) { failures = 0; emit(partialResults) }
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onResults(results: Bundle?) {
            failures = 0
            emit(results)
            restartSoon(recreate = false)   // session ended normally, just re-listen
        }

        override fun onError(error: Int) {
            when (error) {
                // Fatal: no point retrying.
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> fail()
                // Benign: user was just silent — keep listening, don't count as failure.
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restartSoon(recreate = false)
                // Hard errors: the recognizer may be wedged — recreate and back off.
                else -> {
                    failures++
                    if (failures >= MAX_HARD_FAILURES) fail()
                    else restartSoon(recreate = true)
                }
            }
        }
    }
}
