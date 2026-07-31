package hr.azzi.socialgames.alias.Service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The two switches on the settings screen.
 *
 * Kept here rather than in the screen because both are read from where the effect happens —
 * the sound from [SoundSystem], the buzz from the play buttons — and neither of those has
 * any business knowing about a preferences file.
 */
object GameSettings {

    private const val PREFS = "gameSettings"
    private const val SOUND = "soundEffects"
    private const val VIBRATE = "vibrate"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun soundEnabled(context: Context) = prefs(context).getBoolean(SOUND, true)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(SOUND, enabled).apply()
    }

    fun vibrateEnabled(context: Context) = prefs(context).getBoolean(VIBRATE, true)

    fun setVibrateEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(VIBRATE, enabled).apply()
    }

    /** A short tick for a scored word. Silent — and still — if the switch is off. */
    fun tick(context: Context) {
        if (!vibrateEnabled(context)) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
