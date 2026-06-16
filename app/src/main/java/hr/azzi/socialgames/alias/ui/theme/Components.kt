package hr.azzi.socialgames.alias.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PillKind { Primary, Light, Dark }

private val Capsule = RoundedCornerShape(percent = 50)

@Composable
fun PillButton(
    title: String,
    kind: PillKind = PillKind.Primary,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = when (kind) {
        PillKind.Primary -> Alias.accent
        PillKind.Light -> Alias.surface
        PillKind.Dark -> Alias.textPrimary
    }
    val fg = when (kind) {
        PillKind.Primary -> Alias.onAccent
        PillKind.Light -> Alias.blue600
        PillKind.Dark -> Color.White
    }
    val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    Box(
        modifier
            .then(widthModifier)
            .heightIn(min = 56.dp)
            .shadow(8.dp, Capsule, clip = false)
            .clip(Capsule)
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = if (fillWidth) 24.dp else 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = fg, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.4.sp)
    }
}

/** White rounded card with the brand shadow. */
@Composable
fun CCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable () -> Unit) {
    Box(
        modifier
            .shadow(16.dp, CardShape, clip = false)
            .clip(CardShape)
            .background(Alias.surface)
            .padding(padding.dp)
    ) { content() }
}
