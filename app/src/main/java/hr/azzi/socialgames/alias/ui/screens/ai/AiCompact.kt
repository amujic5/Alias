package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * How much the AI play screens have to shrink to fit the height they are given.
 *
 * Both of them are a fixed stack — label, word or clue, bot, microphone, status, score — and
 * the sum of those is taller than a short phone. There is no scrolling to fall back on
 * either: this is a screen you tap in a hurry, and a control that has to be scrolled to is a
 * control that is not there. So everything fixed shrinks together, in proportion, and the
 * empty space between goes first.
 *
 * [COMFORTABLE] is the height the layout was drawn for. Above it nothing changes; below it
 * the whole stack scales, down to a floor past which the microphone stops being an easy
 * target.
 */
@Composable
fun rememberCompactScale(available: Dp): Float = remember(available) {
    (available / COMFORTABLE).coerceIn(MINIMUM_SCALE, 1f)
}

/**
 * What the stack needs at full size: bot and microphone at 180 each, the gaps between them,
 * and the text above and below. Measured off the layout rather than guessed, because the
 * scale is only as good as this number — set too low, the bottom row still falls off the
 * screen, which is exactly what a gentler first attempt did.
 */
private val COMFORTABLE = 740.dp

/** Past this the microphone stops being something you can hit without looking at it. */
private const val MINIMUM_SCALE = 0.62f

/** Scaled, and never smaller than a hairline so a rounding to zero cannot hide something. */
fun Dp.compact(scale: Float): Dp = (this * scale).coerceAtLeast(1.dp)

/**
 * Type shrinks along with everything else. Leaving it fixed is what made the first attempt
 * fall short: the labels, the status and the score come to a couple of hundred points on
 * their own, so a stack that scales around them cannot get small enough.
 */
fun TextUnit.compact(scale: Float): TextUnit = (this.value * scale).sp

fun Int.compactSp(scale: Float): Int = (this * scale).toInt().coerceAtLeast(9)

/**
 * Text that gives up font size rather than lines.
 *
 * The clue and the word are whatever the deck and the model produce, and a long one wrapped
 * past its allowance used to push the microphone and the score off the bottom of the screen.
 * This shrinks until it fits [maxLines], so what it costs is legibility of one line rather
 * than the visibility of everything under it.
 */
@Composable
fun AutoShrinkText(
    text: String,
    maxFontSize: TextUnit,
    maxLines: Int,
    color: Color,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = (maxFontSize.value * 0.55f).sp,
    lineHeightRatio: Float = 1.15f,
    textAlign: TextAlign = TextAlign.Center,
) {
    // Keyed on the text and the ceiling: a new word starts from the top again rather than
    // inheriting the size the last long one settled at.
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = (fontSize.value * lineHeightRatio).sp,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = true,
        onTextLayout = { result ->
            if ((result.hasVisualOverflow || result.lineCount > maxLines) &&
                fontSize.value > minFontSize.value
            ) {
                // A step at a time, so a word that only just overflows keeps almost all of
                // its size instead of jumping to the floor.
                fontSize = (fontSize.value * 0.92f).coerceAtLeast(minFontSize.value).sp
            }
        },
    )
}
