package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.Overline

/** Which background the row is sitting on, since that decides what its text can be. */
enum class CaptureMomentStyle { OnBrand, OnCard }

/**
 * The Capture Moment switch, with the info button that opens the explainer.
 *
 * One definition, used from the game settings before the first round and from the results
 * screen between rounds. Two things differ. The heading, because on results it is deciding
 * about the round that is about to be played rather than the one being set up. And the
 * colours: settings puts it straight on the brand gradient, results puts it in a white card
 * beside the others, and white text on white is not a thing.
 */
@Composable
fun CaptureMomentRow(
    checked: Boolean,
    title: String = stringResource(R.string.capture_moment),
    style: CaptureMomentStyle = CaptureMomentStyle.OnBrand,
    onCheckedChange: (Boolean) -> Unit,
    onInfo: () -> Unit,
) {
    val onCard = style == CaptureMomentStyle.OnCard
    val titleColor = if (onCard) Alias.textSecondary else Color.White.copy(alpha = 0.8f)
    val subtitleColor = if (onCard) Alias.textSecondary else Color.White.copy(alpha = 0.6f)
    val infoTint = if (onCard) Alias.textSecondary else Color.White.copy(alpha = 0.75f)
    val boxFill = if (onCard) Alias.blue600 else Alias.accent
    val tickTint = if (onCard) Color.White else Alias.onAccent
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline(title.uppercase(), color = titleColor)
                Spacer(Modifier.width(2.dp))
                IconButton(onClick = onInfo, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.capture_moment),
                        tint = infoTint,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.capture_moment_subtitle),
                color = subtitleColor, fontFamily = Alias.body,
                fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
                .background(if (checked) boxFill else Color.Transparent)
                .border(2.dp, boxFill, RoundedCornerShape(7.dp))
                .clickable { onCheckedChange(!checked) },
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check, contentDescription = null,
                    tint = tickTint, modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
