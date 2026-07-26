package hr.azzi.socialgames.alias.ai

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.ModelDownloadListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/**
 * Whether speech recognition can actually run for a language, checked before the
 * round instead of discovered from a mid-round error.
 *
 * The recognizer normally works online, so a missing on-device model is not a
 * reason to block: with a network we start straight away and pull the offline
 * model down quietly in the background, so the next round works without one.
 *
 * Only Android 13 (API 33) can be asked any of this — `checkRecognitionSupport`
 * and `triggerModelDownload` arrived there. Below that the only fact available is
 * whether a recognition service exists at all, so anything else stays optimistic
 * and is handled by the runtime error path.
 */
object AISttSupport {

    sealed interface Status {
        /** Recognition can run now. */
        data object Ready : Status
        /** No recognition service on the device — nothing to download, nothing to fix. */
        data object NoService : Status
        /** Offline and no on-device model for this language. */
        data object NeedsInternet : Status
        /** Not offered online nor on-device. */
        data object LanguageUnsupported : Status
    }

    private val main = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { main.post(it) }

    /** Languages a download has already been kicked off for this process. */
    private val prefetched = mutableSetOf<String>()
    /** Held while a download runs so the recognizer isn't collected mid-flight. */
    private var downloader: SpeechRecognizer? = null

    /** Give the platform this long to answer before we stop waiting and proceed. */
    private const val CHECK_TIMEOUT_MS = 2_500L

    fun check(context: Context, locale: String, onResult: (Status) -> Unit) {
        val app = context.applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(app)) { onResult(Status.NoService); return }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Nothing to query on Android 12 and below — assume it works and let the
            // error path speak up if it doesn't.
            onResult(Status.Ready); return
        }
        checkSupported(app, locale, onResult)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkSupported(app: Context, locale: String, onResult: (Status) -> Unit) {
        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(app) }.getOrNull()
        if (recognizer == null) { onResult(Status.Ready); return }

        var done = false
        fun finish(status: Status) {
            if (done) return
            done = true
            runCatching { recognizer.destroy() }
            onResult(status)
        }

        // The support API is optional for a recognition service and some just never
        // answer, so a silent implementation must not strand the player on a gate.
        val timeout = Runnable { finish(Status.Ready) }
        main.postDelayed(timeout, CHECK_TIMEOUT_MS)

        val callback = object : RecognitionSupportCallback {
            override fun onSupportResult(support: RecognitionSupport) {
                main.removeCallbacks(timeout)
                val installed = support.installedOnDeviceLanguages.any { it matches locale }
                val supported = support.supportedOnDeviceLanguages.any { it matches locale }
                val pending = support.pendingOnDeviceLanguages.any { it matches locale }
                val online = support.onlineLanguages.any { it matches locale }
                val connected = isOnline(app)

                // Already usable offline, or usable online right now.
                if (installed || (online && connected)) {
                    if (!installed && !pending && supported) prefetch(app, locale)
                    finish(Status.Ready); return
                }
                finish(
                    when {
                        // Would work, just not without a connection.
                        online || supported || pending -> Status.NeedsInternet
                        else -> Status.LanguageUnsupported
                    }
                )
            }

            override fun onError(error: Int) {
                main.removeCallbacks(timeout)
                finish(Status.Ready)   // can't tell — don't stand in the player's way
            }
        }
        runCatching { recognizer.checkRecognitionSupport(intentFor(locale), mainExecutor, callback) }
            .onFailure { main.removeCallbacks(timeout); finish(Status.Ready) }
    }

    /**
     * Quietly pull down the on-device model so the next round works offline. Never
     * blocks and never reports: the round it was started from is already playing
     * online. Note the platform is free to ask the user to confirm the download.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun prefetch(app: Context, locale: String) {
        if (!prefetched.add(locale)) return
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(app)) return
        val rec = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(app) }.getOrNull() ?: return
        downloader?.let { runCatching { it.destroy() } }
        downloader = rec
        val intent = intentFor(locale)
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                rec.triggerModelDownload(intent, mainExecutor, object : ModelDownloadListener {
                    override fun onProgress(completedPercent: Int) {}
                    override fun onSuccess() { release(rec) }
                    override fun onScheduled() {}          // will finish on its own time
                    override fun onError(error: Int) { prefetched.remove(locale); release(rec) }
                })
            } else {
                // API 33 has no listener: fire it and let go after a grace period.
                rec.triggerModelDownload(intent)
                main.postDelayed({ release(rec) }, 60_000)
            }
        }.isSuccess
        if (!ok) { prefetched.remove(locale); release(rec) }
    }

    private fun release(rec: SpeechRecognizer) {
        runCatching { rec.destroy() }
        if (downloader === rec) downloader = null
    }

    private fun intentFor(locale: String) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        }

    private fun isOnline(app: Context): Boolean {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Tags come back as "hr-HR", "hr_HR" or bare "hr" depending on the service. */
    private infix fun String.matches(locale: String): Boolean {
        val a = replace('_', '-').lowercase()
        val b = locale.replace('_', '-').lowercase()
        return a == b || a.substringBefore('-') == b.substringBefore('-')
    }
}
