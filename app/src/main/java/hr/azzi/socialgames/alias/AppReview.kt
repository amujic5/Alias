package hr.azzi.socialgames.alias

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play In-App Review — asks the user to rate the app inline (no leaving the app).
 * Triggered after an AI game finishes, once the interstitial is dismissed and the score
 * is on screen.
 *
 * Google enforces its own display quota, so the sheet is shown at most a few times per
 * user per period regardless of how often this is called — over quota it silently no-ops.
 * Never assume it appeared.
 */
object AppReview {

    fun request(context: Context) {
        val activity = context.findActivity() ?: return
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            if (activity.isFinishing || activity.isDestroyed) return@addOnCompleteListener
            manager.launchReviewFlow(activity, task.result)
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
