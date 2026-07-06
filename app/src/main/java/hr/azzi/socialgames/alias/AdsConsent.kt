package hr.azzi.socialgames.alias

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google User Messaging Platform (UMP) consent flow — Android counterpart of the iOS
 * AdsConsentManager. Gathers GDPR consent (EEA / UK) via the UMP SDK and starts the
 * Mobile Ads SDK only once ads may be requested. Ad code must check [canRequestAds]
 * before loading — personalized vs. non-personalized serving is then decided by the
 * SDK from the gathered consent, with no per-request flags.
 *
 * https://developers.google.com/admob/android/privacy
 */
object AdsConsent {

    private var consentInformation: ConsentInformation? = null
    private var adsInitialized = false

    /** Whether ads may be requested yet (consent gathered, or not required in this region). */
    val canRequestAds: Boolean
        get() = consentInformation?.canRequestAds() ?: false

    /** Entry point — call once from the launcher Activity. Safe to call each launch. */
    fun gather(activity: Activity) {
        val info = consentInformation
            ?: UserMessagingPlatform.getConsentInformation(activity).also { consentInformation = it }

        // Consent obtained in a previous session lets us start serving ads right away,
        // while any (needed) form loads in the background.
        if (info.canRequestAds()) initializeAds(activity)

        // To test the EEA form on a device, force the geography with debug settings:
        // val debug = ConsentDebugSettings.Builder(activity)
        //     .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //     .addTestDeviceHashedId("<TEST_DEVICE_HASHED_ID>") // from logcat on first run
        //     .build()
        // val params = ConsentRequestParameters.Builder().setConsentDebugSettings(debug).build()
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Present the consent form if the user's consent is required and not yet gathered.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (info.canRequestAds()) initializeAds(activity)
                }
            },
            { /* requestConsentInfoUpdate failed — keep whatever consent state we already have */ },
        )
    }

    private fun initializeAds(context: Context) {
        if (adsInitialized) return
        adsInitialized = true
        MobileAds.initialize(context.applicationContext) {}
    }
}
