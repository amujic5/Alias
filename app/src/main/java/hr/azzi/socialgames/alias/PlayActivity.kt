package hr.azzi.socialgames.alias

import android.Manifest
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Service.Analytics
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.serializedName
import hr.azzi.socialgames.alias.Service.NoAds
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundCardInfo
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundCardWord
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundMediaLibrary
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundRecorder
import hr.azzi.socialgames.alias.Service.SoundSystem
import hr.azzi.socialgames.alias.ui.screens.PlayScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme
import java.io.File

class PlayActivity : BaseActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var mInterstitialAd: InterstitialAd? = null
    private var mInterstitialAd2: InterstitialAd? = null

    private val game: Game by lazy { intent.getParcelableExtra<Game>("game") as Game }
    private val soundSystem: SoundSystem by lazy { SoundSystem(this) }

    /** Set in Settings, and only ever true on a device that passed `DualCameraSupport`. */
    private val captureMoment: Boolean by lazy { intent.getBooleanExtra("captureMoment", false) }
    private var recorder: RoundRecorder? = null

    /** True once the cameras are actually rolling, which is what puts the stage on screen. */
    private var filming by mutableStateOf(false)

    /** Goes false if the device turns out to manage only one camera at a time. */
    private var hasFrontCamera by mutableStateOf(true)

    private var roundStartLogged = false

    /**
     * Asked for once, here, rather than in Settings: this is the screen where the answer
     * matters, and where a player can see what it is for. A refusal is not fatal — the round
     * is played without a clip.
     */
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.CAMERA] == true) startRecording()
        else Log.w(TAG, "camera declined — playing the round without a clip")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log()
        startRecordingIfAsked()
        loadInterstitialAd()
        if (game._currentTeamIndex % 2 == 1) loadInterstitialAd2()

        setComposeContent {
            AliasTheme {
                PlayScreen(
                    game = game,
                    soundSystem = soundSystem,
                    onFinished = { showResults() },
                    onQuit = { finish() },
                    onRoundRunning = { running ->
                        recorder?.setRoundRunning(running)
                        if (running) logRoundStarted()
                    },
                    onRoundState = { word, correct, skipped, remaining ->
                        recorder?.update(word, correct, skipped, remaining)
                    },
                    cameraMode = filming,
                    onPreviewSurface = { surface, width, height ->
                        recorder?.setPreviewSurface(surface, width, height)
                    },
                    hasFrontCamera = hasFrontCamera,
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    /**
     * The clip is a bonus, never a precondition: if the camera is unavailable or the
     * permission was declined, the round is played exactly as it would have been.
     */
    private fun startRecordingIfAsked() {
        if (!captureMoment) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            startRecording()
        } else {
            // The microphone is asked for alongside, but only the camera decides whether the
            // clip happens at all — a silent clip is still worth having.
            permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun startRecording() {
        hasFrontCamera = true
        recorder = RoundRecorder(this)
            .apply { onSingleCamera = { runOnUiThread { hasFrontCamera = false } } }
            .takeIf { it.start(cardInfo()) }
        filming = recorder != null
    }

    /**
     * What the intro and outro cards say. Read twice — before the round for the intro, and
     * again after it for the outro, when the word lists exist.
     */
    private fun cardInfo() = RoundCardInfo(
        teamName = game.currentTeam.teamName,
        explainer = game.explainingPlayerName,
        guesser = game.answeringPlayerName,
        // A word carries its value onto the cards on the decks that price them; the skipped
        // ones carry theirs too, drawn muted — passing on a 3 is a different miss from a 1.
        correctWords = game.currentTeamMarkedWords.filter { it.isCorrect }
            .map { RoundCardWord(it.word, if (game.isScored) it.points else null) },
        skippedWords = game.currentTeamMarkedWords.filterNot { it.isCorrect }
            .map { RoundCardWord(it.word, if (game.isScored) it.points else null) },
    )

    /**
     * Logged once per round, at the moment it actually begins — not at every resume from a
     * pause, which is the same signal arriving again.
     *
     * What is reported is whether the round is really being filmed, not whether the switch
     * was on: a round where the camera was asked for and refused is not a recorded round.
     */
    private fun logRoundStarted() {
        if (roundStartLogged) return
        roundStartLogged = true
        Analytics.log(
            this,
            if (recorder != null) Analytics.RECORDING_ROUND_ENABLED
            else Analytics.RECORDING_ROUND_DISABLED,
        )
    }

    /**
     * Closes the clip and hands the file back.
     *
     * Off the main thread on purpose: finishing means writing two seconds of outro, laying
     * the whistle down and closing an mp4, which is the better part of a second. Blocking the
     * UI for it would stall the very screen transition it is meant to precede.
     */
    private fun stopRecording(onDone: (File?) -> Unit) {
        val recording = recorder
        if (recording == null) {
            onDone(null)
            return
        }
        recorder = null
        filming = false
        val info = cardInfo()
        val dualCamera = !recording.singleCamera
        Thread {
            recording.stop(info) { file ->
                // Moved out of the cache and into the library, which is what makes it
                // survive: the cache is the system's to reclaim, and nothing else keeps it.
                val kept = file?.let {
                    RoundMediaLibrary.save(
                        context = this,
                        video = it,
                        teamName = info.teamName,
                        explainer = info.explainer,
                        guesser = info.guesser,
                        correctCount = info.correctTotal,
                        skippedCount = info.skippedWords.size,
                    )?.let { item -> RoundMediaLibrary.fileFor(this, item) }
                }
                Log.w(TAG, "round clip: ${kept?.absolutePath ?: "none"} (${kept?.length() ?: 0} bytes)")
                Analytics.logReplayCreation(this, succeeded = kept != null, dualCamera = dualCamera)
                runOnUiThread { onDone(kept) }
            }
        }.start()
    }

    override fun onDestroy() {
        // A round abandoned partway still has to let the cameras go; nobody is waiting for
        // the file, so it is only closed so the next round can open them again.
        stopRecording {}
        super.onDestroy()
    }

    private fun log() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        bundle.putString("language", DictionaryService.playingDictionary?.language)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        val language = DictionaryService.playingDictionary?.language ?: "unknown"
        firebaseAnalytics.logEvent(language, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
            putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        })
    }

    private fun loadInterstitialAd() {
        if (NoAds.isPurchased) return
        if (!AdsConsent.canRequestAds) return
        MobileAds.initialize(this) {}
        val prod = "ca-app-pub-1489905432577426/3906492992"
        InterstitialAd.load(this, prod, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) { mInterstitialAd = null }
            override fun onAdLoaded(p0: InterstitialAd) { mInterstitialAd = p0 }
        })
    }

    private fun loadInterstitialAd2() {
        if (NoAds.isPurchased) return
        if (!AdsConsent.canRequestAds) return
        MobileAds.initialize(this) {}
        val prod = "ca-app-pub-1489905432577426/4633082957"
        InterstitialAd.load(this, prod, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) { mInterstitialAd2 = null }
            override fun onAdLoaded(p0: InterstitialAd) { mInterstitialAd2 = p0 }
        })
    }

    private fun showResults() {
        stopRecording { clip -> goToResults(clip) }
    }

    private fun goToResults(clip: File?) {
        // Every finished round passes here; Quit leaves through finish() instead, so an
        // abandoned round is not counted.
        DictionaryService.playingDictionary?.let { dictionary ->
            Analytics.logRoundPlayed(
                this,
                DictionaryService.playingBoardGame?.id?.serializedName() ?: "unknown",
                dictionary.languageCode,
            )
        }

        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("game", game)
        intent.putExtra("replayPath", clip?.absolutePath)
        // Carried on so the next round is filmed too. The choice is made once, in Settings,
        // and the game runs through Play → Results → Play without passing that way again.
        intent.putExtra("captureMoment", captureMoment)
        intent.flags = FLAG_ACTIVITY_NO_ANIMATION
        startActivity(intent)
        finish()

        mInterstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd2?.let { it.show(this@PlayActivity) }
                }
            }
            Handler().postDelayed({ ad.show(this) }, 1000)
        }
    }

    private companion object {
        const val TAG = "RoundVideo"
    }
}
