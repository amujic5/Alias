package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * What a card in a scored deck is worth, shown while it is being explained.
 *
 * Two backgrounds, two treatments. On the play screen it sits on white, where a soft tint
 * carries it; on the camera stage it sits over a moving picture, where the same tint alone
 * would disappear on a bright frame — so that one is stronger and outlined.
 */
enum class DifficultyPillStyle { OnLight, OnDark }

/** Green, orange, pink — read as a ramp and already in the app's palette. */
fun difficultyTint(points: Int): Color = when (points) {
    1 -> Color(0xFF34C759)
    2 -> Color(0xFFFF8A3D)
    else -> Color(0xFFFF5C8A)
}

/**
 * Points are 1..3; anything else is treated as the hardest tier rather than hidden, so a
 * stray value still reads as "this one is worth more".
 */
@Composable
fun DifficultyPill(
    points: Int,
    modifier: Modifier = Modifier,
    style: DifficultyPillStyle = DifficultyPillStyle.OnLight,
) {
    val tint = difficultyTint(points)
    val onDark = style == DifficultyPillStyle.OnDark
    val tier = when (points.coerceIn(1, 3)) {
        1 -> R.string.emh_tier_1
        2 -> R.string.emh_tier_2
        else -> R.string.emh_tier_3
    }
    var shape = modifier
        .clip(CircleShape)
        .background(tint.copy(alpha = if (onDark) 0.22f else 0.14f))
    if (onDark) shape = shape.border(1.5.dp, tint.copy(alpha = 0.55f), CircleShape)

    Row(
        shape.padding(horizontal = if (onDark) 14.dp else 18.dp, vertical = if (onDark) 9.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(if (onDark) 8.dp else 10.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Text(
            stringResource(tier),
            color = tint,
            fontFamily = Alias.body,
            fontWeight = FontWeight.Black,
            fontSize = if (onDark) 13.sp else 15.sp,
            letterSpacing = if (onDark) 1.0.sp else 1.2.sp,
            modifier = Modifier.padding(start = if (onDark) 8.dp else 10.dp),
        )
    }
}

/**
 * The same badge shrunk to a row: no tier name, just the number. Used by the Edit Answers
 * list, where the word it belongs to is right there.
 */
@Composable
fun PointsBadge(points: Int, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val tint = difficultyTint(points)
    Box(
        modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = if (dimmed) 0.06f else 0.14f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$points",
            color = tint.copy(alpha = if (dimmed) 0.35f else 1f),
            fontFamily = Alias.body,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 0.4.sp,
        )
    }
}

/**
 * What a word did to the score, signed: `+1`, `+2`, or `−1` in red for a skip. For decks
 * where the row is about the total rather than the word's tier (After Dark).
 */
@Composable
fun DeltaBadge(delta: Int, modifier: Modifier = Modifier) {
    val tint = if (delta < 0) Alias.danger else difficultyTint(delta)
    Box(
        modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (delta < 0) "\u2212${-delta}" else "+$delta",
            color = tint,
            fontFamily = Alias.body,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 0.4.sp,
        )
    }
}
