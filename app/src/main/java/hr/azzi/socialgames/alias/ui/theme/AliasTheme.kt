package hr.azzi.socialgames.alias.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    val onAccent = Color(0xFF0E1726)
    val textPrimary = Color(0xFF0E1726)
    val textSecondary = Color(0xFF7A8699)
    val blue600 = Color(0xFF0F70D1)
    val success = Color(0xFF34C759)
    val danger = Color(0xFFFF453A)
    val surface = Color.White
    val divider = Color(0xFFE2E8F1)
    val fieldBg = Color(0xFFEEF3F9)

    val confetti = listOf(
        Color(0xFFFF5C8A), Color(0xFFFF8A3D), Color(0xFF4CD964),
        Color(0xFF9B6DFF), Color(0xFF28D9C4), Color(0xFFFFD23D)
    )

    val radius = 20.dp
    // Type — Baloo 2 for display/headlines/logo, Plus Jakarta Sans for body/labels.
    val display = Baloo2
    val body = PlusJakarta
}

private val colorScheme = lightColorScheme(
    primary = Alias.accent,
    onPrimary = Alias.onAccent,
    surface = Alias.surface,
    onSurface = Alias.textPrimary,
    background = Alias.brandBottom,
)

@Composable
fun AliasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Full-screen brand gradient background. */
@Composable
fun BrandBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().background(Alias.brandGradient)) { content() }
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
