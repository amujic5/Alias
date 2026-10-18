package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import hr.azzi.socialgames.alias.Models.AfterDarkSegment
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

/*
 * The pieces of the Play screen only the After Dark deck has: its colours, the segment
 * named above the word, the card that shows how far the round has climbed, and the burst
 * a CLIMAX answer sets off. Port of iOS `AfterDarkViews.swift`.
 */

/**
 * After Dark's palette.
 *
 * Fixed, not the adaptive tokens: the deck is a night-time deck and looks the same on a
 * light phone and a dark one. The colours belong to the theme, not to the appearance.
 */
object AfterDarkStyle {
    val background = Color(0xFF14111F)
    val glow = Color(0xFF2A2142)
    val surface = Color(0xFF241D33)
    val well = Color(0xFF3A3150)
    val text = Color.White
    val textMuted = Color(0xFFB9B2C9)
    val accent = Color(0xFFA98BFF)
    val climax = Color(0xFFFF6FA3)
    val timer = Color(0xFFFFD27A)
    val skip = Color(0xFF2E2640)
    val correct = Color(0xFFC8F27C)
    val onCorrect = Color(0xFF14111F)
    /** The countdown behind the word: there, but well behind it. */
    val countdown = Color.White.copy(alpha = 0.06f)
}

/** The near-black backdrop with a soft violet glow where the word sits. */
@Composable
fun AfterDarkBackdrop(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        Box(
            Modifier.fillMaxSize().background(AfterDarkStyle.background).background(
                Brush.radialGradient(
                    listOf(AfterDarkStyle.glow, AfterDarkStyle.background),
                    center = Offset(w * 0.5f, h * 0.48f),
                    radius = maxOf(w, h) * 0.6f,
                )
            )
        )
    }
}

/** The segment the word on screen comes from, spaced out above it: "PRIVLAČNOST". */
@Composable
fun AfterDarkSegmentLabel(segment: AfterDarkSegment?, modifier: Modifier = Modifier) {
    if (segment == null) return
    Text(
        stringResource(segment.titleRes).uppercase(),
        color = if (segment == AfterDarkSegment.CLIMAX) AfterDarkStyle.climax else AfterDarkStyle.accent,
        fontFamily = Alias.body,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 4.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 20.dp),
    )
}

/**
 * How far the round has climbed: the level's name, one bar per segment, and what is left
 * to reach CLIMAX — or, once there, what every hit is now worth.
 */
@Composable
fun AfterDarkProgress(level: AfterDarkSegment, hitsToClimax: Int, modifier: Modifier = Modifier) {
    val all = AfterDarkSegment.values()
    val reached = level.ordinal + 1
    val fill = if (level == AfterDarkSegment.CLIMAX) AfterDarkStyle.climax else AfterDarkStyle.accent
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(AfterDarkStyle.surface)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(level.titleRes).uppercase(), color = AfterDarkStyle.textMuted,
                fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.size(12.dp))
            Text("$reached / ${all.size}", color = AfterDarkStyle.textMuted,
                fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            all.forEachIndexed { index, _ ->
                Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(5.dp))
                    .background(if (index < reached) fill else AfterDarkStyle.well))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (level == AfterDarkSegment.CLIMAX)
                stringResource(R.string.after_dark_at_climax, AfterDarkSegment.CLIMAX.points)
            else
                stringResource(R.string.after_dark_to_climax, hitsToClimax, AfterDarkSegment.CLIMAX.points),
            color = AfterDarkStyle.textMuted, fontFamily = Alias.body, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The burst that goes off on every correct CLIMAX answer.
 *
 * Laid behind the word and never in the way of a tap: the round keeps going over it, and
 * the next answer can land while it is still playing — in which case it simply starts
 * again from the top rather than queueing. Each new value of [trigger] is one burst; zero
 * is none.
 */
@Composable
fun AfterDarkClimaxBurst(trigger: Int, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.after_dark_climax))
    val animatable = rememberLottieAnimatable()
    LaunchedEffect(trigger, composition) {
        if (trigger == 0 || composition == null) return@LaunchedEffect
        // A beat quicker than authored, so it is over before the next word is read out.
        animatable.animate(composition, speed = 1.2f)
    }
    if (trigger != 0 && animatable.isPlaying) {
        LottieAnimation(composition, { animatable.progress }, modifier.size(380.dp).alpha(0.8f))
    }
}
