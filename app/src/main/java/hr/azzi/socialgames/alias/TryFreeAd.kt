package hr.azzi.socialgames.alias

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Rewarded video behind the "Try for free" button on paid decks: watch the ad, then
 * play the deck for this session without buying it (no permanent unlock — the paywall
 * reappears next time). Mirrors the iOS AdRewarded.
 *
 * Uses Google's test unit in debug builds (so dev builds never serve/click live ads,
 * which would violate AdMob policy) and the production unit in release.
 */
object TryFreeAd {

    private const val PROD_UNIT = "ca-app-pub-1489905432577426/3073917715"
    private const val TEST_UNIT = "ca-app-pub-3940256099942544/5224354917" // Google sample rewarded
    private val unitId: String get() = if (BuildConfig.DEBUG) TEST_UNIT else PROD_UNIT

    private var ad: RewardedAd? = null
    private var loading = false
    private var initialized = false

    val isReady: Boolean get() = ad != null

    /** Request a rewarded ad in the background (no-op if one is ready or loading). */
    fun preload(context: Context) {
        if (ad != null || loading) return
        val app = context.applicationContext
        if (!initialized) { MobileAds.initialize(app) {}; initialized = true }
        loading = true
        RewardedAd.load(app, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { loading = false; ad = null }
            override fun onAdLoaded(loaded: RewardedAd) { loading = false; ad = loaded }
        })
    }

    /**
     * Show the rewarded ad, then invoke [onReward] iff the user earned the reward.
     * Fails open: if no ad can be shown (empty inventory / load failure), [onReward] is
     * called anyway so the user is never trapped out of the deck they asked to try.
     */
    fun show(activity: Activity, onReward: () -> Unit) {
        val ready = ad
        if (ready != null) {
            ad = null
            present(activity, ready, onReward)
            return
        }
        // Not ready — try to load now, then present; fail open if the load fails.
        val app = activity.applicationContext
        if (!initialized) { MobileAds.initialize(app) {}; initialized = true }
        loading = true
        RewardedAd.load(app, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { loading = false; ad = null; onReward() }
            override fun onAdLoaded(loaded: RewardedAd) {
                loading = false
                if (activity.isFinishing || activity.isDestroyed) { ad = loaded; return }
                present(activity, loaded, onReward)
            }
        })
    }

    private fun present(activity: Activity, rewarded: RewardedAd, onReward: () -> Unit) {
        var earned = false
        rewarded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { if (earned) onReward(); preload(activity) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { onReward(); preload(activity) }
        }
        rewarded.show(activity) { earned = true }
    }
}
