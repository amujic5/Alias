package hr.azzi.socialgames.alias.ai

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import hr.azzi.socialgames.alias.BuildConfig

/**
 * Full-screen interstitial shown after an AI round (Practice + Challenge), mirroring
 * the iOS AdInterstitial: preload while playing, present ~0.8s after the round ends.
 *
 * Uses Google's test unit in debug builds (so dev builds never serve/click live ads,
 * which would violate AdMob policy) and the production unit in release.
 */
object AIInterstitial {

    private const val PROD_UNIT = "ca-app-pub-1489905432577426/1224402586"
    private const val TEST_UNIT = "ca-app-pub-3940256099942544/1033173712" // Google sample interstitial
    private val unitId: String get() = if (BuildConfig.DEBUG) TEST_UNIT else PROD_UNIT

    private var ad: InterstitialAd? = null
    private var loading = false
    private var initialized = false

    /** Request an interstitial in the background (no-op if one is ready or loading). */
    fun preload(context: Context) {
        if (ad != null || loading) return
        val app = context.applicationContext
        if (!initialized) { MobileAds.initialize(app) {}; initialized = true }
        loading = true
        InterstitialAd.load(app, unitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { loading = false; ad = null }
            override fun onAdLoaded(loaded: InterstitialAd) { loading = false; ad = loaded }
        })
    }

    /** Present the ad ~0.8s after the round ends (over the result screen), if one is ready. */
    fun show(context: Context) {
        val activity = context as? Activity ?: return
        val current = ad ?: return
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { ad = null; preload(activity) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { ad = null; preload(activity) }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed) current.show(activity)
        }, 800)
    }
}
