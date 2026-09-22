package hr.azzi.socialgames.alias.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.getSystemService
import hr.azzi.socialgames.alias.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * The two reasons the app is allowed to interrupt somebody.
 *
 * Android port of iOS `AliasNotifications.swift`. Both are *local*: nothing is sent from a
 * server, nothing needs a token, and a player who never grants permission is simply never
 * scheduled. Each is switched on and off on its own in Settings, because "stop nagging me
 * about the Arena" and "stop suggesting game night" are different requests.
 */
enum class AliasNotificationKind(val raw: String, val slots: Int) {
    /** Two days without an Arena round, at seven in the evening. Three, then it stops. */
    ARENA("arena", 3),

    /** Friday and Saturday afternoon: is tonight Alias night? Four weeks of both. */
    ALIAS_NIGHT("aliasNight", 8);

    /**
     * Base for the alarm request codes, so one kind can be cancelled without touching the
     * other. Derived, never discovered — see [AliasNotifications.cancel].
     */
    val requestBase: Int get() = if (this == ARENA) 4100 else 4200

    /** What tapping it should open. */
    val target: String get() = if (this == ARENA) "arena" else "home"

    val enabledKey: String get() = "notif_${raw}_enabled"
}

/**
 * Schedules the local reminders, and reschedules them whenever the facts change.
 *
 * Android cannot run code to decide, on the evening of the second day, whether somebody
 * has played — and neither can iOS. So nothing is decided at fire time: a window of alarms
 * is scheduled in advance and thrown away and rebuilt every time the app comes forward or
 * a round is played. The pending list is therefore always derived from the current truth.
 *
 * `setAndAllowWhileIdle` rather than an exact alarm: exact alarms need
 * `SCHEDULE_EXACT_ALARM` from API 31, which is a permission the Play Console asks you to
 * justify, and a reminder that lands within a few minutes of seven is still a reminder.
 */
object AliasNotifications {

    // MARK: Tunables

    /** Days without an Arena round before the first nudge. */
    const val ARENA_IDLE_DAYS = 2

    /**
     * And the gap between that one and the next, for somebody who still does not come
     * back. Three total, then it stops — a fourth is not a reminder, it is nagging.
     */
    const val ARENA_REPEAT_DAYS = 7
    const val ARENA_HOUR = 19

    /** Friday and Saturday — `Calendar`'s numbering, where Sunday is 1. */
    val NIGHT_WEEKDAYS = listOf(Calendar.FRIDAY, Calendar.SATURDAY)
    const val NIGHT_HOUR = 14

    /**
     * How many weeks ahead to fill. Each occurrence is scheduled separately rather than as
     * one repeating alarm, which is what lets consecutive weekends say different things: a
     * repeating alarm carries one body forever.
     */
    const val NIGHT_WEEKS = 4

    /** How many wordings each kind has. See `notif_*_title_N` in the strings files. */
    const val VARIANTS = 3

    const val CHANNEL_ID = "alias_reminders"

    private const val PREFS = "alias_notifications"
    private const val KEY_LAST_ARENA = "notif_arena_last_played"
    private const val KEY_ASKED = "notif_permission_asked"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // MARK: Settings

    /**
     * Both default to **on**, and neither can do anything without system permission — so
     * the defaults describe what happens once somebody says yes, not what happens to
     * somebody who never did.
     */
    fun isEnabled(context: Context, kind: AliasNotificationKind): Boolean =
        prefs(context).getBoolean(kind.enabledKey, true)

    fun setEnabled(context: Context, kind: AliasNotificationKind, enabled: Boolean) {
        prefs(context).edit { putBoolean(kind.enabledKey, enabled) }
        refresh(context)
    }

    // MARK: Permission

    var hasAskedPermission: (Context) -> Boolean = { prefs(it).getBoolean(KEY_ASKED, false) }

    fun markAsked(context: Context) = prefs(context).edit { putBoolean(KEY_ASKED, true) }

    /** Whether the system will actually show anything we post. */
    fun isAllowed(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

    // MARK: Facts

    /**
     * Records that an Arena round was played, and pushes the nudges out two days.
     *
     * Called when a round is submitted rather than when the Arena is opened: the reminder
     * is about playing, and somebody who opens the lobby and leaves has not.
     */
    fun recordArenaPlayed(context: Context, at: Long = System.currentTimeMillis()) {
        prefs(context).edit { putLong(KEY_LAST_ARENA, at) }
        refresh(context)
    }

    private fun lastArenaPlay(context: Context): Long? =
        prefs(context).getLong(KEY_LAST_ARENA, 0L).takeIf { it > 0 }

    // MARK: Scheduling

    /**
     * Throws away everything pending and rebuilds it from what is true right now.
     *
     * Cheap, idempotent, and safe to call often — which is the point, because it is the
     * only thing keeping the schedule honest.
     */
    fun refresh(context: Context, now: Long = System.currentTimeMillis()) {
        ensureChannel(context)
        AliasNotificationKind.entries.forEach { cancel(context, it) }
        if (!isAllowed(context)) return

        if (isEnabled(context, AliasNotificationKind.ARENA)) {
            schedule(context, AliasNotificationKind.ARENA, arenaDates(context, now))
        }
        if (isEnabled(context, AliasNotificationKind.ALIAS_NIGHT)) {
            schedule(context, AliasNotificationKind.ALIAS_NIGHT, nightDates(now))
        }
    }

    /**
     * Every request code a kind can occupy.
     *
     * Listed rather than discovered. Android has no way to read back what is pending, so a
     * cancel that guessed would leave alarms behind for as long as the install lasts.
     */
    private fun cancel(context: Context, kind: AliasNotificationKind) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        // The variant is irrelevant here: a `PendingIntent` matches on the request code
        // and the intent's action and component, never on its extras. Any variant finds
        // the same alarm.
        repeat(kind.slots) { slot ->
            alarms.cancel(pendingIntent(context, kind, slot, variant = 1))
        }
    }

    private fun schedule(context: Context, kind: AliasNotificationKind, dates: List<Long>) {
        val alarms = context.getSystemService<AlarmManager>() ?: return

        // A random starting wording, then one along for each following date. Random so two
        // devices do not read the same thing on the same evening; sequential after that so
        // the same player is never told the same thing twice in a row.
        val start = (0 until VARIANTS).random()

        dates.forEachIndexed { index, whenMs ->
            val variant = (start + index) % VARIANTS + 1
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                whenMs,
                pendingIntent(context, kind, index, variant),
            )
        }
    }

    private fun pendingIntent(
        context: Context,
        kind: AliasNotificationKind,
        slot: Int,
        variant: Int,
    ): PendingIntent {
        val intent = Intent(context, AliasNotificationReceiver::class.java).apply {
            action = "hr.azzi.socialgames.alias.REMINDER"
            putExtra(AliasNotificationReceiver.EXTRA_KIND, kind.raw)
            putExtra(AliasNotificationReceiver.EXTRA_VARIANT, variant)
            putExtra(AliasNotificationReceiver.EXTRA_SLOT, slot)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, kind.requestBase + slot, intent, flags)
    }

    // MARK: Dates

    /**
     * Evenings to nudge an idle Arena player.
     *
     * Counted from the last round, not from the last launch: opening the app without
     * playing does not reset it, which is the difference between "come back" and "come
     * play".
     */
    private fun arenaDates(context: Context, now: Long): List<Long> {
        val base = lastArenaPlay(context) ?: now
        var first = at(hour = ARENA_HOUR, onDay = base + TimeUnit.DAYS.toMillis(ARENA_IDLE_DAYS.toLong()))
        // Already past — somebody who has been away far longer than two days. Tonight if
        // there is still time, otherwise tomorrow.
        if (first <= now) first = nextTime(ARENA_HOUR, now)

        return (0 until AliasNotificationKind.ARENA.slots).map {
            first + TimeUnit.DAYS.toMillis((it * ARENA_REPEAT_DAYS).toLong())
        }
    }

    /** The next few Friday and Saturday afternoons, in order. */
    private fun nightDates(now: Long): List<Long> =
        NIGHT_WEEKDAYS.flatMap { weekday -> occurrences(weekday, NIGHT_HOUR, NIGHT_WEEKS, now) }.sorted()

    private fun at(hour: Int, onDay: Long): Long = Calendar.getInstance().apply {
        timeInMillis = onDay
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun nextTime(hour: Int, after: Long): Long {
        val today = at(hour, after)
        return if (today > after) today else today + TimeUnit.DAYS.toMillis(1)
    }

    private fun occurrences(weekday: Int, hour: Int, count: Int, after: Long): List<Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = after
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != weekday || timeInMillis <= after) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val out = mutableListOf<Long>()
        repeat(count) {
            out += cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 7)
        }
        return out
    }

    // MARK: Channel

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.settings_notifications),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }
}
