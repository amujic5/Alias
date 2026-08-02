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
import hr.azzi.socialgames.alias.AdsConsent
import hr.azzi.socialgames.alias.BuildConfig
import hr.azzi.socialgames.alias.Service.NoAds

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
        if (NoAds.isPurchased) return
        if (ad != null || loading || !AdsConsent.canRequestAds) return
        val app = context.applicationContext
        if (!initialized) { MobileAds.initialize(app) {}; initialized = true }
        loading = true
        InterstitialAd.load(app, unitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { loading = false; ad = null }
            override fun onAdLoaded(loaded: InterstitialAd) { loading = false; ad = loaded }
        })
    }

    /**
     * Present the ad ~0.8s after the round ends (over the result screen), if one is ready.
     * [onDismiss] runs once the ad is dismissed — or immediately if there is no ad to show —
     * so callers can chain a follow-up (e.g. the in-app review prompt) after the ad.
     */
    fun show(context: Context, onDismiss: () -> Unit = {}) {
        // Bought out of ads — hand straight back, so whatever was chained after the ad
        // (the rating prompt) still happens.
        if (NoAds.isPurchased) { onDismiss(); return }
        val activity = context as? Activity
        val current = ad
        if (activity == null || current == null) { onDismiss(); return }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { ad = null; preload(activity); onDismiss() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { ad = null; preload(activity); onDismiss() }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed) current.show(activity) else onDismiss()
        }, 800)
    }
}
