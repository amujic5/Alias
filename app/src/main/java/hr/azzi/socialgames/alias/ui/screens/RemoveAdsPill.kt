package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

private val Ink = Color(0xFF0E1726)
private val Gold = Color(0xFFFFD23D)

/**
 * The Remove Ads offer in the top bar (Figma "01 Home · Croatia · Remove Ads", node 3272:633).
 *
 * Two states in one control. Collapsed it is a disc the size of the settings gear opposite
 * it, so the two read as a pair; expanded it grows to the right into a pill saying what the
 * offer is and what it costs. What sets it apart from the gear is the fill and the gold ring
 * — it is an offer rather than another piece of chrome, and has to be noticed once.
 *
 * @param price straight from Play, so it is right in every currency. Without it the row says
 *   what kind of purchase this is and leaves the number out rather than inventing one.
 */
@Composable
fun RemoveAdsPill(
    expanded: Boolean,
    price: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            // The width follows the content, so collapsing is the same animation as growing.
            .animateContentSize(tween(durationMillis = 350))
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(if (expanded) Ink.copy(alpha = 0.6f) else Color.Transparent)
            .border(
                width = if (expanded) 1.5.dp else 0.dp,
                color = if (expanded) Color.White.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(25.dp),
            )
            .clickable { onClick() }
            .padding(start = if (expanded) 5.dp else 0.dp, end = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (expanded) 10.dp else 0.dp),
    ) {
        // Collapsed, the badge is the whole control — the pill's fill and border go with the
        // copy, leaving a disc rather than a stub of a pill.
        NoAdsBadge(size = if (expanded) 40.dp else 50.dp)

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    stringResource(R.string.remove_ads_title),
                    color = Color.White, fontFamily = Alias.body,
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                )
                Text(
                    if (price != null) {
                        stringResource(R.string.remove_ads_subtitle_priced, price)
                    } else {
                        stringResource(R.string.remove_ads_subtitle)
                    },
                    color = Color.White.copy(alpha = 0.72f), fontFamily = Alias.body,
                    fontWeight = FontWeight.Medium, fontSize = 11.sp,
                )
            }
        }
    }
}

/** The gold "no ads" mark: a ring around the word, struck through. */
@Composable
private fun NoAdsBadge(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.6f))
            .border(2.dp, Gold, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "ADS",
            color = Gold, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = size.value.times(0.25f).sp,
        )
        // Corner to corner, inset so the ends stop short of the ring.
        Canvas(Modifier.size(size)) {
            val inset = this.size.width * 0.18f
            drawLine(
                color = Gold,
                start = Offset(inset, this.size.height - inset),
                end = Offset(this.size.width - inset, inset),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
