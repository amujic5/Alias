package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaMode
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.PillButton
import kotlin.math.roundToInt

/** One word from the round, and what became of it. */
data class ArenaReviewWord(val text: String, val guessed: Boolean)

/**
 * The gap between finishing a round and seeing the leaderboard.
 *
 * Android port of iOS `ArenaRoundReviewView.swift`.
 *
 * Scoring cannot start until the submission window shuts, so there are several seconds
 * after the last word in which the board does not exist yet. Showing a spinner for them
 * wastes the most interesting moment of the round — the one where the player still
 * remembers every word and wants to know how they did.
 *
 * Every figure here comes from the round the player just played, not from a read: the
 * client knows what it submitted.
 */
@Composable
fun ArenaRoundReviewScreen(
    mode: ArenaMode,
    words: List<ArenaReviewWord>,
    secondsToResults: Int,
    onStay: () -> Unit,
    onLeave: () -> Unit,
) {
    val correct = words.count { it.guessed }
    val skipped = words.size - correct

    /**
     * What the board actually ranks on: one point a word, minus one for every word let go.
     *
     * The server has always scored it this way and this screen never said so. It showed
     * "12 correct" and "4 missed" as two facts of equal weight, and then the leaderboard
     * said 8 — a number the player had no way to derive, which reads as the skips having
     * been forgiven and something else having gone wrong. Passing on a word is a choice
     * with a price, and a price nobody is quoted is not one.
     *
     * Negative on purpose. A round spent skipping is worse than a round not played.
     */
    val net = correct - skipped

    /** Share of the words reached that were guessed. Zero reached is 0%, not 100%. */
    val accuracy = if (words.isEmpty()) 0f else correct.toFloat() / words.size

    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        Column(Modifier.fillMaxSize()) {
            Header(mode, secondsToResults)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScoreCard(correct, skipped, net, accuracy)
                WordsCard(words)
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PillButton(title = stringResource(R.string.arena_results_stay), onClick = onStay)
            Text(
                stringResource(R.string.arena_results_leave),
                fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLeave).padding(8.dp),
            )
        }
    }
}

@Composable
private fun Header(mode: ArenaMode, secondsToResults: Int) {
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(mode.badgeRes),
            fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
            letterSpacing = 1.1.sp, color = Color.White.copy(alpha = 0.75f), maxLines = 1,
        )
        Text(
            stringResource(R.string.arena_results_title),
            fontFamily = Alias.display, fontSize = 28.sp, color = Color.White,
            maxLines = 2, textAlign = TextAlign.Center,
        )
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(ArenaPalette.onBrandPill)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.Schedule, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.arena_review_results_in, secondsToResults),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                color = Color.White, maxLines = 1,
            )
        }
    }
}

@Composable
private fun ScoreCard(correct: Int, skipped: Int, net: Int, accuracy: Float) {
    ArenaCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ArenaSectionLabel(stringResource(R.string.arena_review_your_score))
                // The arithmetic in full, so the price of a skip is on the screen where
                // the skip is being counted.
                Text(
                    stringResource(R.string.arena_review_net_sub, correct, skipped),
                    fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 1,
                )
            }
            Text(
                "$net",
                fontFamily = Alias.display, fontSize = 34.sp,
                color = if (net < 0) ArenaColors.loss else ArenaColors.title, maxLines = 1,
            )
        }

        Spacer(Modifier.height(14.dp))
        Tally(
            value = correct,
            title = stringResource(R.string.arena_review_correct),
            subtitle = stringResource(R.string.arena_review_correct_sub),
            tint = Alias.success, background = ArenaPalette.chipGood, good = true,
        )
        Spacer(Modifier.height(10.dp))
        Tally(
            value = skipped,
            title = stringResource(R.string.arena_review_skipped),
            subtitle = stringResource(R.string.arena_review_skipped_sub),
            tint = Color(0xFFE4573D), background = ArenaPalette.chipWarn, good = false,
        )

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.arena_review_accuracy),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
                color = ArenaColors.body, maxLines = 1,
            )
            Box(
                Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(50))
                    .background(ArenaColors.well),
            ) {
                Box(
                    Modifier.fillMaxWidth(accuracy).height(7.dp)
                        .clip(RoundedCornerShape(50)).background(Alias.success),
                )
            }
            Text(
                "${(accuracy * 100).roundToInt()}%",
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                color = ArenaColors.title, maxLines = 1,
            )
        }
    }
}

@Composable
private fun Tally(value: Int, title: String, subtitle: String, tint: Color, background: Color, good: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (good) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            null, tint = tint, modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Alias.display, fontSize = 16.sp, color = ArenaColors.title, maxLines = 1)
            Text(subtitle, fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 1)
        }
        Text("$value", fontFamily = Alias.display, fontSize = 24.sp, color = tint, maxLines = 1)
    }
}

@Composable
private fun WordsCard(words: List<ArenaReviewWord>) {
    ArenaCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArenaSectionLabel(stringResource(R.string.arena_review_words_title))
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.arena_review_words_count, words.size),
                fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 1,
            )
        }
        words.forEachIndexed { index, word ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (word.guessed) Alias.success else Color(0xFFE4573D)),
                )
                Text(
                    word.text,
                    fontFamily = Alias.display, fontSize = 15.sp, color = ArenaColors.title,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (!word.guessed) {
                    Text(
                        stringResource(R.string.arena_review_word_skipped),
                        fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
                        color = ArenaColors.body, maxLines = 1,
                    )
                }
            }
            if (index < words.size - 1) ArenaRowDivider()
        }
        Spacer(Modifier.height(6.dp))
    }
}
