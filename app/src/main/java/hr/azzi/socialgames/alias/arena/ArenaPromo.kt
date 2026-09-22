package hr.azzi.socialgames.alias.arena

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

/**
 * The showing rules for the Arena promo.
 *
 * Android port of iOS `ArenaPromo.swift`.
 *
 * Once on the first launch, then every seven days until the player either takes it or has
 * been asked five times. After that it is finished, permanently, on this device.
 *
 * The counter only moves when the sheet is actually put on screen, so a launch where it
 * was skipped — something else was up, the app was too old — costs nothing. Five asks
 * means five asks.
 */
object ArenaPromo {

    /** Shown at most this many times before the offer is retired. */
    const val MAX_SHOWS = 5

    /** The wait between one refusal and the next ask. */
    const val INTERVAL_DAYS = 7L

    private const val PREFS = "alias_arena_promo"
    private const val KEY_SHOWS = "arena_promo_shows"
    private const val KEY_LAST = "arena_promo_last_shown"
    private const val KEY_ACCEPTED = "arena_promo_accepted"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Whether the sheet is due.
     *
     * The first launch has no last-shown, which is the case that shows immediately; every
     * one after it is a date comparison. A clock moved backwards cannot bring the prompt
     * forward, because a negative interval fails the same test.
     */
    fun isDue(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val p = prefs(context)
        if (p.getBoolean(KEY_ACCEPTED, false)) return false
        if (p.getInt(KEY_SHOWS, 0) >= MAX_SHOWS) return false
        val last = p.getLong(KEY_LAST, 0L)
        if (last == 0L) return true
        return now - last >= TimeUnit.DAYS.toMillis(INTERVAL_DAYS)
    }

    /** Counted when it is on screen, not when it is asked for. */
    fun recordShown(context: Context, now: Long = System.currentTimeMillis()) {
        prefs(context).edit {
            putInt(KEY_SHOWS, prefs(context).getInt(KEY_SHOWS, 0) + 1)
            putLong(KEY_LAST, now)
        }
    }

    fun recordAccepted(context: Context) {
        prefs(context).edit { putBoolean(KEY_ACCEPTED, true) }
    }

    /** Forgets everything, so the next launch offers it again. For testing. */
    fun reset(context: Context) {
        prefs(context).edit { clear() }
    }
}
