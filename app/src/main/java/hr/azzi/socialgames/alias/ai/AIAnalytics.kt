package hr.azzi.socialgames.alias.ai

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics events for the Play-with-AI / AI-Challenge flow on Android.
 * Centralised so event names + params stay consistent. Mirrors the iOS AIAnalytics
 * table, but every event name is prefixed with "android_" so the platforms are
 * distinguishable in the Firebase console.
 */
object AIAnalytics {

    private fun log(context: Context, name: String, params: Bundle? = null) {
        FirebaseAnalytics.getInstance(context.applicationContext).logEvent(name, params)
    }

    private fun configParams(config: AIPracticeConfig) = Bundle().apply {
        putString("language", config.language.code)
        putInt("seconds", config.totalSeconds)
    }

    /** A Practice round was started. Separate event per mode; language + seconds as params. */
    fun practiceOpen(context: Context, config: AIPracticeConfig) {
        val name = if (config.mode == AIMode.AI_EXPLAINS) "android_ai_practice_open_ai_explains"
        else "android_ai_practice_open_you_explain"
        log(context, name, configParams(config))
    }

    /** The user started creating a challenge (their own round). Separate event per mode. */
    fun challengeCreateOpen(context: Context, config: AIPracticeConfig) {
        val name = if (config.mode == AIMode.AI_EXPLAINS) "android_ai_challenge_create_ai_explains"
        else "android_ai_challenge_create_you_explain"
        log(context, name, configParams(config))
    }

    /** The Share button on a challenge (rank board or outcome screen) was tapped. */
    fun challengeShareTapped(context: Context, challengeId: String) {
        log(context, "android_ai_challenge_share_tapped", Bundle().apply { putString("challenge_id", challengeId) })
    }

    /** App opened via the custom URL scheme (aliaswords://challenge/{id}). */
    fun appOpenViaScheme(context: Context) = log(context, "android_app_open_scheme")

    /** App opened via an App Link (https://aliaswords.com/challenge/{id}). */
    fun appOpenViaUniversalLink(context: Context) = log(context, "android_app_open_universal_link")
}
