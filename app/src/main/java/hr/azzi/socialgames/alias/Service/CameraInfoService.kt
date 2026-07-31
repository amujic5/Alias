package hr.azzi.socialgames.alias.Service

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * How often the dual-camera explainer sheet opens by itself. Ported from the iOS
 * `CameraInfoService` — the first few visits always get it, after that it comes back
 * occasionally so a player who ignored it early still learns the feature exists.
 */
object CameraInfoService {

    private const val PREFS = "dualCamera"
    private const val SHOWN_COUNT = "camera_info_shown_count"
    private const val LAST_SHOWN = "camera_info_last_shown_date"

    private const val MAX_SHOW_COUNT = 3
    private const val MIN_DAYS_BETWEEN_SHOWS = 5L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldShowInfo(context: Context): Boolean {
        val prefs = prefs(context)
        val count = prefs.getInt(SHOWN_COUNT, 0)
        if (count < MAX_SHOW_COUNT || count % 5 == 0) return true

        val last = prefs.getLong(LAST_SHOWN, 0L)
        if (last == 0L) return true
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - last)
        return days >= MIN_DAYS_BETWEEN_SHOWS
    }

    fun infoShowed(context: Context) {
        val prefs = prefs(context)
        prefs.edit()
            .putInt(SHOWN_COUNT, prefs.getInt(SHOWN_COUNT, 0) + 1)
            .putLong(LAST_SHOWN, System.currentTimeMillis())
            .apply()
    }
}
