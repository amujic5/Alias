package hr.azzi.socialgames.alias.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import hr.azzi.socialgames.alias.arena.isDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Brand design tokens — mirror the iOS `CTheme` / Figma so the Android UI matches.
 */
object Alias {
    // Brand gradient
    val brandTop = Color(0xFF3AA0F5)
    val brandBottom = Color(0xFF0F70D1)
    val brandGradient = Brush.verticalGradient(listOf(brandTop, brandBottom))

    val accent = Color(0xFFFFD23D)
    val accentPressed = Color(0xFFF7BE1E)
    val blue600 = Color(0xFF0F70D1)
    val success = Color(0xFF34C759)
    val danger = Color(0xFFFF453A)

    /**
     * Fixed on purpose: ink on the accent, which is the same yellow in both appearances.
     * Making it adaptive would put white text on a yellow button at night.
     */
    val onAccent = Color(0xFF0E1726)

    // The adaptive half. Each of these is a role — what a card is painted, what a heading
    // is painted — and resolves against the appearance in force, so a screen that already
    // reads `Alias.surface` follows the System / Light / Dark switch without changing.
    //
    // Composable getters rather than two palettes passed around: every call site is
    // already inside a composable, and this way the switch reaches all of them at once.

    /** A card or sheet on the brand backdrop. */
    val surface: Color @Composable get() = dyn(Color.White, Color(0xFF12365E))

    val textPrimary: Color @Composable get() = dyn(Color(0xFF0E1726), Color.White)
    val textSecondary: Color @Composable get() = dyn(Color(0xFF7A8699), Color.White.copy(alpha = 0.62f))
    val divider: Color @Composable get() = dyn(Color(0xFFE2E8F1), Color.White.copy(alpha = 0.12f))

    /** A field, a well, the track behind a control. */
    val fieldBg: Color @Composable get() = dyn(Color(0xFFEEF3F9), Color.White.copy(alpha = 0.10f))

    /**
     * The backdrop everything is drawn on. Blue by day, navy by night — never white, so
     * white text on brand furniture stays legible in both.
     */
    val brandGradientAdaptive: Brush
        @Composable get() = Brush.verticalGradient(
            if (isDarkTheme()) listOf(Color(0xFF0E2A4B), Color(0xFF091C33))
            else listOf(brandTop, brandBottom)
        )

    @Composable private fun dyn(light: Color, dark: Color) = if (isDarkTheme()) dark else light

    val confetti = listOf(
        Color(0xFFFF5C8A), Color(0xFFFF8A3D), Color(0xFF4CD964),
        Color(0xFF9B6DFF), Color(0xFF28D9C4), Color(0xFFFFD23D)
    )

    val radius = 20.dp
    // Type — Baloo 2 for display/headlines/logo, Plus Jakarta Sans for body/labels.
    val display = Baloo2
    val body = PlusJakarta
}

/**
 * Material's own scheme, built per appearance.
 *
 * The app barely uses Material components — its surfaces are its own — but a dialog or a
 * text field that does reach for the scheme should not come back white at night.
 */
private val colorScheme: androidx.compose.material3.ColorScheme
    @Composable get() = if (isDarkTheme()) {
        darkColorScheme(
            primary = Alias.accent,
            onPrimary = Alias.onAccent,
            surface = Alias.surface,
            onSurface = Alias.textPrimary,
            background = Color(0xFF091C33),
        )
    } else {
        lightColorScheme(
            primary = Alias.accent,
            onPrimary = Alias.onAccent,
            surface = Alias.surface,
            onSurface = Alias.textPrimary,
            background = Alias.brandBottom,
        )
    }

@Composable
fun AliasTheme(content: @Composable () -> Unit) {
    // Provided here rather than per-activity: the switch in Settings has to reach every
    // screen, and an activity that forgot to wrap would silently stay light.
    val appearance = hr.azzi.socialgames.alias.arena.AppAppearance
        .load(androidx.compose.ui.platform.LocalContext.current)
    androidx.compose.runtime.CompositionLocalProvider(
        hr.azzi.socialgames.alias.arena.LocalAppAppearance provides appearance.value
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/** Full-screen brand gradient background. */
@Composable
fun BrandBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().background(Alias.brandGradientAdaptive)) { content() }
}

// MARK: text helpers

@Composable
fun DisplayText(
    text: String,
    size: Int,
    color: Color = Color.White,
    weight: FontWeight = FontWeight.ExtraBold,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier = modifier, color = color, fontFamily = Alias.display,
        fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.1).sp)
}

@Composable
fun Overline(text: String, color: Color = Color.White.copy(alpha = 0.8f), modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = color, fontFamily = Alias.body, fontWeight = FontWeight.Black,
        fontSize = 12.sp, letterSpacing = 2.5.sp)
}

val CardShape = RoundedCornerShape(Alias.radius)
