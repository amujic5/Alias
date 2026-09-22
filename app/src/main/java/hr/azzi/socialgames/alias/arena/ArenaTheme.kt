package hr.azzi.socialgames.alias.arena

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * What the player chose in Settings.
 *
 * Android port of iOS `AppearanceManager.swift`. Held in one place rather than read from
 * preferences at every call site, because the switch has to repaint what is already on
 * screen — a value read once at composition would leave the current screen light until it
 * was navigated away from and back.
 */
enum class AppAppearance(val raw: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        private const val PREFS = "alias_appearance"
        private const val KEY = "app_appearance"

        fun from(raw: String?): AppAppearance = entries.firstOrNull { it.raw == raw } ?: SYSTEM

        /**
         * The live choice. A `MutableState` so a change recomposes everything reading
         * [isDarkTheme]; persisted through [set] so it survives the process.
         */
        private var state: MutableState<AppAppearance>? = null

        fun load(context: Context): MutableState<AppAppearance> = state ?: run {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            mutableStateOf(from(prefs.getString(KEY, null))).also { state = it }
        }

        fun set(context: Context, value: AppAppearance) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, value.raw).apply()
            load(context).value = value
        }
    }
}

/** The appearance in force, so a composable can ask without reaching for a Context. */
val LocalAppAppearance = compositionLocalOf { AppAppearance.SYSTEM }

/** Whether to paint dark: the player's choice, or the phone's when they have not made one. */
@Composable
@ReadOnlyComposable
fun isDarkTheme(): Boolean = when (LocalAppAppearance.current) {
    AppAppearance.SYSTEM -> isSystemInDarkTheme()
    AppAppearance.LIGHT -> false
    AppAppearance.DARK -> true
}

/**
 * The AI and Arena palette, in both appearances.
 *
 * Android port of iOS `AIDarkTheme.swift`. Every token here is a role, not a colour —
 * [card] is "what a card is painted", [title] is "what a heading is painted" — and each
 * resolves against the appearance in force, so these screens follow the same
 * System / Light / Dark switch as the rest of the app.
 *
 * The iOS type is called `AIDark` for historical reasons: those screens were drawn dark
 * first. There is no such history here, so it is named for what it is.
 */
object ArenaColors {

    @Composable private fun dyn(light: Color, dark: Color) = if (isDarkTheme()) dark else light

    val backdrop: Brush
        @Composable get() = Brush.verticalGradient(
            if (isDarkTheme()) listOf(Color(0xFF0E2A4B), Color(0xFF091C33))
            else listOf(Color(0xFF3AA0F5), Color(0xFF0F70D1))
        )

    /**
     * A card on the backdrop. Solid in daylight; translucent at night, so the gradient
     * still shows through and the screen does not read as a stack of flat rectangles.
     */
    val card: Color @Composable get() = dyn(Color.White, Color(0xFF12365E).copy(alpha = 0.55f))
    val border: Color @Composable get() = dyn(Color(0xFFE2E8F1), Color.White.copy(alpha = 0.10f))

    /** A control well — the track behind a segmented row, a settings pill. */
    val well: Color @Composable get() = dyn(Color(0xFFEEF3F9), Color.White.copy(alpha = 0.10f))

    val title: Color @Composable get() = dyn(Color(0xFF0E1726), Color.White)
    val body: Color @Composable get() = dyn(Color(0xFF7A8699), Color.White.copy(alpha = 0.62f))

    /**
     * A spinner's tint.
     *
     * White reads on the gradient in both appearances, because the gradient is blue or
     * navy either way. On a *card* it does not: the card is white in daylight, and a white
     * spinner on it is an empty hole where the wait should be.
     */
    val spinner: Color @Composable get() = dyn(Color(0xFF7A8699), Color.White)

    val win: Color @Composable get() = dyn(Color(0xFF34C759), Color(0xFF4ADE80))
    val loss: Color @Composable get() = dyn(Color(0xFFFF453A), Color(0xFFFF7A7A))
}

/**
 * The Arena's fixed accents.
 *
 * Not adaptive, and not by omission: these sit on the brand gradient or on a coloured
 * chip, which are the same in both appearances. Making them follow the theme would give a
 * pink LIVE badge one pink by day and another by night for no reason.
 */
object ArenaPalette {
    val entryBadge = Color(0xFFFF9F1C)
    val live = Color(0xFFFF5C8A)
    val onBrandPill = Color(0xFF0063B8)
    val modeGlyph = Color(0xFF9B6DFF)
    val readyDot = Color(0xFF3FD98B)

    /** Gold, lilac, bronze — first, second, third. */
    val podium = listOf(Alias.accent, Color(0xFFC9B8FF), Color(0xFFFFB36B))

    /** The tint behind your own row, wherever a list has one. */
    val youRow = Alias.accent.copy(alpha = 0.18f)

    val chipGood = Alias.success.copy(alpha = 0.20f)
    val chipWarn = Color(0xFFFF7A7A).copy(alpha = 0.20f)
}

/** `m:ss`, the only clock format the Arena uses. */
fun arenaMMSS(seconds: Int): String {
    val s = maxOf(0, seconds)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
