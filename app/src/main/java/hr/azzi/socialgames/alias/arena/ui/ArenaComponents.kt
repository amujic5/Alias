package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.AliasAvatar
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaMode
import hr.azzi.socialgames.alias.arena.arenaMMSS
import hr.azzi.socialgames.alias.ui.theme.Alias
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column

/**
 * The pieces every Arena screen is built from.
 *
 * Android port of iOS `ArenaComponents.swift`.
 */

/** The face a player wears, with an optional ring. */
@Composable
fun ArenaAvatarImage(
    id: String?,
    size: androidx.compose.ui.unit.Dp,
    ring: Color? = Color.White,
    ringWidth: androidx.compose.ui.unit.Dp = 2.dp,
    /** Drains the colour from a locked face, so the padlock is not the only signal. */
    desaturated: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val avatar = AliasAvatar.named(id)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .then(if (ring != null) Modifier.border(ringWidth, ring, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(avatar.drawable),
            contentDescription = stringResource(avatar.nameRes),
            contentScale = ContentScale.Crop,
            colorFilter = if (desaturated) {
                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                )
            } else null,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    }
}

/**
 * The lobby's big countdown: a track, an accent arc that drains, and `m:ss` inside.
 *
 * [showsTime] because the lobby draws its own caption and clock inside the ring — two
 * clocks on one ring was the first thing anybody noticed about that screen.
 */
@Composable
fun ArenaCountdownRing(
    seconds: Int,
    total: Int,
    diameter: androidx.compose.ui.unit.Dp = 174.dp,
    showsTime: Boolean = true,
) {
    val fraction = if (total <= 0) 0f else (seconds.toFloat() / total).coerceIn(0f, 1f)
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Alias.accent,
                startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (showsTime) {
            Text(
                arenaMMSS(seconds),
                fontFamily = Alias.display,
                fontSize = 34.sp,
                color = Color.White,
            )
        }
    }
}

/** A small upper-case pill. */
@Composable
fun ArenaBadge(
    text: String,
    background: Color,
    foreground: Color,
    size: Int = 11,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        fontFamily = Alias.body,
        fontWeight = FontWeight.Black,
        fontSize = size.sp,
        color = foreground,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** The tracked, upper-case caption above a block. */
@Composable
fun ArenaSectionLabel(text: String, color: Color = ArenaColors.body, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontFamily = Alias.body,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 1.1.sp,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Guesser / Explainer — which of the two standings is on screen.
 *
 * The two are genuinely different games: one is measured on words you named from a clue,
 * the other on words the AI named from your description. Ranking them together would
 * average two skills that do not trade off, so the server never did — this is just the
 * control that finally says which one you are reading.
 */
@Composable
fun ArenaModeSwitcher(mode: ArenaMode, onChange: (ArenaMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ArenaColors.well)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ArenaMode.entries.forEach { option ->
            val on = mode == option
            Text(
                stringResource(option.roleNameRes),
                fontFamily = Alias.body,
                fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (on) Alias.onAccent else ArenaColors.body,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Alias.accent else Color.Transparent)
                    .clickable { onChange(option) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

/** "You guess" / "AI guesses" — what *you* do, not what the round is called. */
val ArenaMode.titleRes: Int
    get() = if (this == ArenaMode.GUESS) R.string.arena_mode_guess_title else R.string.arena_mode_clue_title

val ArenaMode.subtitleRes: Int
    get() = if (this == ArenaMode.GUESS) R.string.arena_mode_guess_subtitle else R.string.arena_mode_clue_subtitle

/** Upper-case form for the badge over the leaderboard. */
val ArenaMode.badgeRes: Int
    get() = if (this == ArenaMode.GUESS) R.string.arena_mode_badge_guess else R.string.arena_mode_badge_clue

/**
 * What the player *is* on that board, rather than what the round does — a standing
 * belongs to a person, and "Guesser" names one where "You guess" names a turn.
 */
val ArenaMode.roleNameRes: Int
    get() = if (this == ArenaMode.GUESS) R.string.arena_role_guesser else R.string.arena_role_explainer

/** A card on the Arena backdrop: the adaptive fill, a hairline border, rounded. */
@Composable
fun ArenaCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    radius: androidx.compose.ui.unit.Dp = 22.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(ArenaColors.card)
            .border(1.dp, ArenaColors.border, RoundedCornerShape(radius))
            .padding(padding),
        content = content,
    )
}

/** The hairline between two entries in a list card. */
@Composable
fun ArenaRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(ArenaColors.border),
    )
}
