package hr.azzi.socialgames.alias

import android.app.Application
import hr.azzi.socialgames.alias.Service.Billing

/** App entry — installs the variant-specific Firebase App Check provider. */
class AliasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { AppCheckSetup.install() }
        runCatching { Billing.start(this) }
        // The level, from launch rather than from the first visit to the AI section: the
        // deck screen gates on it and is two taps from Home. See `ArenaIdentity`.
        runCatching { hr.azzi.socialgames.alias.arena.ArenaIdentity.start(this) }
    }

    /**
     * Refuses Stream's notification-permission requester.
     *
     * `io.getstream.android.push.permissions.PushNotificationPermissionRequester` watches
     * every activity in the process, and on each `onActivityStopped` reads
     * `findViewById(android.R.id.content)` through a non-null Kotlin type. When an activity
     * is stopped without a content view in place the read returns null and the app dies
     * inside `Application.dispatchActivityStopped` — a crash in a stop callback, so nothing
     * of ours is even on the stack to catch it. It is unfixed as of the current release
     * (stream-android-push 1.3.4), so there is no version to upgrade to.
     *
     * Two things install it: an androidx.startup initializer that runs for every user at
     * launch (removed in the manifest), and `StreamVideoBuilder.build()`, which creates it
     * unconditionally when Online Play starts a call. Both arrive here, which is why the
     * refusal sits at this one point rather than at either call site.
     *
     * Nothing is lost. What the requester exists to do is ask for POST_NOTIFICATIONS on
     * Stream's behalf, and this app registers no push device generators — it joins calls
     * from inside the app over Firestore and has no ringing flow, which is also why the
     * manifest already strips Stream's full-screen-intent permission.
     */
    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) {
        if (callback != null &&
            callback.javaClass.name.startsWith("io.getstream.android.push.permissions.")
        ) {
            return
        }
        super.registerActivityLifecycleCallbacks(callback)
    }
}
