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
 * Full-screen interstitial shown after a round: preload while playing, present ~0.8s
 * after it ends.
 *
 * An instance per ad unit rather than one object, because the round screens are shared —
 * Practice, a Challenge and an Arena round are all played on them — and the unit is not.
 * While this was a singleton every Arena round reported its impression against the AI
 * Practice unit.
 *
 * Google's test unit in debug builds, so dev builds never serve or click a live ad, which
 * would violate AdMob policy.
 */
class AdInterstitial(private val prodUnit: String) {

    companion object {
        private const val TEST_UNIT = "ca-app-pub-3940256099942544/1033173712" // Google sample

        /** Practice and Challenge — the AI section's own rounds. */
        val ai = AdInterstitial("ca-app-pub-1489905432577426/1224402586")

        /** Alias Arena round end. */
        val arena = AdInterstitial("ca-app-pub-1489905432577426/7533530102")

        private var initialized = false
    }

    private val unitId: String get() = if (BuildConfig.DEBUG) TEST_UNIT else prodUnit

    private var ad: InterstitialAd? = null
    private var loading = false

    /** Request an interstitial in the background (no-op if one is ready or loading). */
    fun preload(context: Context) {
        if (NoAds.isActive) return
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
        // Out of the ads, bought or earned — hand straight back, so whatever was
        // chained after the ad still happens.
        if (NoAds.isActive) { onDismiss(); return }
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
