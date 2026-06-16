package hr.azzi.socialgames.alias

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Bundle
import android.os.Handler
import androidx.activity.compose.setContent
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.SoundSystem
import hr.azzi.socialgames.alias.ui.screens.PlayScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class PlayActivity : BaseActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var mInterstitialAd: InterstitialAd? = null
    private var mInterstitialAd2: InterstitialAd? = null

    private val game: Game by lazy { intent.getParcelableExtra<Game>("game") as Game }
    private val soundSystem: SoundSystem by lazy { SoundSystem(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log()
        loadInterstitialAd()
        if (game._currentTeamIndex % 2 == 1) loadInterstitialAd2()

        setContent {
            AliasTheme {
                PlayScreen(
                    game = game,
                    soundSystem = soundSystem,
                    onFinished = { showResults() },
                    onQuit = { finish() },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
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
        MobileAds.initialize(this) {}
        val prod = "ca-app-pub-1489905432577426/3906492992"
        InterstitialAd.load(this, prod, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) { mInterstitialAd = null }
            override fun onAdLoaded(p0: InterstitialAd) { mInterstitialAd = p0 }
        })
    }

    private fun loadInterstitialAd2() {
        MobileAds.initialize(this) {}
        val prod = "ca-app-pub-1489905432577426/4633082957"
        InterstitialAd.load(this, prod, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) { mInterstitialAd2 = null }
            override fun onAdLoaded(p0: InterstitialAd) { mInterstitialAd2 = p0 }
        })
    }

    private fun showResults() {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("game", game)
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
}
