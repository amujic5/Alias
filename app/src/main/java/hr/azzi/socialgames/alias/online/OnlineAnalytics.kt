package hr.azzi.socialgames.alias.online

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics events for the Online Play flow — Android counterpart of iOS
 * `OnlineAnalytics.swift`. Event names mirror iOS exactly but with an `Android` suffix
 * instead of `iOS`, so both platforms' data lines up per-platform in the console.
 */
object OnlineAnalytics {

    private fun log(context: Context, name: String) {
        FirebaseAnalytics.getInstance(context.applicationContext).logEvent(name, null)
    }

    /**
     * The Online Play screen was opened — the top of the funnel, logged before a room
     * exists so the drop-off to created/joined is visible. Unlike the room events this
     * one follows the iOS `ios_`/`android_` prefix convention, not the suffix one.
     */
    fun screenOpen(context: Context) = log(context, "android_online_screen_open")

    fun roomCreated(context: Context) = log(context, "onlineRoomCreatedAndroid")
    fun roomJoined(context: Context) = log(context, "onlineRoomJoinedAndroid")
    /** Split by room size: onlineRoomStartedAndroid4 / …6 / …8. */
    fun roomStarted(context: Context, maxPlayers: Int) = log(context, "onlineRoomStartedAndroid$maxPlayers")
    fun roundFinished(context: Context) = log(context, "onlineRoundFinishedAndroid")
    fun roomFinished(context: Context) = log(context, "onlineRoomFinishedAndroid")
    fun roomCancelled(context: Context) = log(context, "onlineRoomCancelledAndroid")
}
