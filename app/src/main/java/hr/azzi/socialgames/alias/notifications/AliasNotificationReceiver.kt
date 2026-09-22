package hr.azzi.socialgames.alias.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import hr.azzi.socialgames.alias.ArenaActivity
import hr.azzi.socialgames.alias.HomeActivity
import hr.azzi.socialgames.alias.R

/**
 * Builds and posts a reminder when its alarm fires.
 *
 * Android port of the delegate half of iOS `AliasNotifications.swift`. The text is chosen
 * at schedule time and carried in the intent, not decided here: the variant has to be
 * fixed when the window is built so consecutive reminders can differ, and a receiver that
 * picked at random would undo that.
 */
class AliasNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_VARIANT = "variant"
        const val EXTRA_SLOT = "slot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val raw = intent.getStringExtra(EXTRA_KIND) ?: return
        val kind = AliasNotificationKind.entries.firstOrNull { it.raw == raw } ?: return
        val variant = intent.getIntExtra(EXTRA_VARIANT, 1)
        val slot = intent.getIntExtra(EXTRA_SLOT, 0)

        // Switched off, or permission withdrawn since this was scheduled. The alarm
        // outlives both, so the check has to happen here as well as at schedule time.
        if (!AliasNotifications.isEnabled(context, kind)) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) return

        val titleRes = stringId(context, "notif_${kind.raw}_title_$variant")
        val bodyRes = stringId(context, "notif_${kind.raw}_body_$variant")
        if (titleRes == 0 || bodyRes == 0) return

        AliasNotifications.ensureChannel(context)

        /**
         * `home` opens nothing in particular: tapping it has already done the only thing
         * it was asking for, which is to bring the app forward. Pushing a screen on top of
         * that would be deciding for somebody who has not chosen yet.
         */
        val target = if (kind.target == "arena") {
            Intent(context, ArenaActivity::class.java)
        } else {
            Intent(context, HomeActivity::class.java)
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

        val pending = PendingIntent.getActivity(
            context,
            kind.requestBase + slot,
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, AliasNotifications.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(bodyRes)))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(kind.requestBase + slot, notification)
        }
    }

    /**
     * Looked up by name because the variant is a number chosen at schedule time.
     *
     * The alternative is a `when` over every variant of every kind, which is the same
     * table written twice — once here and once in the strings file — and the two would
     * drift the first time a fourth wording was added.
     */
    private fun stringId(context: Context, name: String): Int =
        context.resources.getIdentifier(name, "string", context.packageName)
}
