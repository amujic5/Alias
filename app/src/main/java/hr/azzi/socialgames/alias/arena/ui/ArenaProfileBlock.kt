package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaPeriod
import hr.azzi.socialgames.alias.arena.ArenaProfileStore
import hr.azzi.socialgames.alias.arena.ArenaStanding
import hr.azzi.socialgames.alias.ui.theme.Alias
import java.text.NumberFormat

/**
 * The Arena's half of the profile.
 *
 * Android port of the Arena sections of iOS `PlayerProfileView.swift`.
 *
 * Folded into the app's existing profile rather than given a screen of its own. iOS
 * briefly had two profiles describing the same person and they disagreed about the
 * avatar; one screen is the fix, and this is the block that makes it one.
 */
@Composable
fun ArenaProfileIdentity(
    profile: ArenaProfileStore,
    onPickAvatar: () -> Unit,
    onOpenRewards: () -> Unit,
) {
    val avatar by profile.avatar.collectAsState()
    val summary by profile.summary.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            ArenaAvatarImage(avatar, 120.dp, ring = Alias.accent, ringWidth = 5.dp,
                modifier = Modifier.clickable(onClick = onPickAvatar))
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Alias.accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Edit, null, tint = Alias.onAccent, modifier = Modifier.size(16.dp)) }
        }

        Text(
            stringResource(R.string.arena_profile_level, summary.level),
            fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.92f),
        )

        /**
         * Progress through the current level, against this level's own span.
         *
         * The curve widens — level three costs 2 017 XP and level ten 3 445 — so a flat
         * thousand was only ever right up to level two, and past it the bar and the level
         * disagreed openly.
         */
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.width(200.dp).height(8.dp).clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(summary.levelProgress).height(8.dp)
                        .clip(RoundedCornerShape(50)).background(Alias.accent),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.arena_profile_xp,
                    number(summary.xpIntoLevel), number(summary.xpForLevel),
                ),
                fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        /**
         * The way into the ladder, put where the question comes up.
         *
         * The bar says how far along this level is and answers nothing about what is at
         * the end of it. The level-up screen has a link, but that is four seconds long
         * and only appears when a level lands — this is the door for every other moment.
         */
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Alias.accent)
                .clickable(onClick = onOpenRewards)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.CardGiftcard, null, tint = Alias.onAccent, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.arena_profile_rewards),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                color = Alias.onAccent, maxLines = 1,
            )
        }
    }
}

/**
 * Where you stand, on whichever of the three boards you are looking at.
 *
 * The mode switcher sits above the period toggle because it is the bigger question: the
 * two modes are different games, and mixing their standings would average two skills that
 * do not trade off.
 */
@Composable
fun ArenaProfileRanking(
    profile: ArenaProfileStore,
    onOpenStandings: () -> Unit,
) {
    val mode by profile.mode.collectAsState()
    val loading by profile.loadingRanks.collectAsState()
    val daily by profile.daily.collectAsState()
    val weekly by profile.weekly.collectAsState()
    val monthly by profile.monthly.collectAsState()

    var period by remember { mutableStateOf(ArenaPeriod.WEEKLY) }
    val standing = when (period) {
        ArenaPeriod.DAILY -> daily
        ArenaPeriod.WEEKLY -> weekly
        ArenaPeriod.MONTHLY -> monthly
    }

    ArenaCard(Modifier.fillMaxWidth()) {
        ArenaSectionLabel(stringResource(R.string.arena_profile_ranking))
        Spacer(Modifier.height(12.dp))
        ArenaModeSwitcher(mode, onChange = { profile.setMode(it) })
        Spacer(Modifier.height(10.dp))
        PeriodToggle(period) { period = it }
        Spacer(Modifier.height(14.dp))

        Box(Modifier.fillMaxWidth().heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
            if (loading) {
                // On a card, not on the gradient: a white spinner here would be an empty
                // hole where the wait should be.
                CircularProgressIndicator(color = ArenaColors.spinner, modifier = Modifier.size(28.dp))
            } else {
                StandingSummary(standing, period)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(ArenaColors.well)
                .clickable(onClick = onOpenStandings)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.arena_standings_title),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                color = ArenaColors.title, maxLines = 1, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ChevronRight, null, tint = ArenaColors.body, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StandingSummary(standing: ArenaStanding, period: ArenaPeriod) {
    val rank = standing.rank
    if (rank == null) {
        Text(
            stringResource(R.string.arena_standings_not_played),
            fontFamily = Alias.body, fontSize = 13.sp, color = ArenaColors.body,
            textAlign = TextAlign.Center,
        )
        return
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    when (period) {
                        ArenaPeriod.DAILY -> R.string.arena_profile_rank_daily
                        ArenaPeriod.WEEKLY -> R.string.arena_profile_rank_weekly
                        ArenaPeriod.MONTHLY -> R.string.arena_profile_rank_monthly
                    }
                ),
                fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = ArenaColors.body, maxLines = 1,
            )
            Text(
                "#$rank",
                fontFamily = Alias.display, fontSize = 32.sp, color = ArenaColors.title, maxLines = 1,
            )
            standing.topPercent?.let {
                Text(
                    stringResource(R.string.arena_profile_percentile, it, standing.players),
                    fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 1,
                )
            }
        }
        // Only when the player has actually moved themselves — see `withDelta`.
        standing.delta?.let { delta ->
            Text(
                stringResource(
                    if (delta > 0) R.string.arena_profile_delta_up else R.string.arena_profile_delta_down,
                    kotlin.math.abs(delta),
                ),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
                color = if (delta > 0) Alias.success else Alias.danger, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(
                        (if (delta > 0) Alias.success else Alias.danger).copy(alpha = 0.16f)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * Daily / weekly / monthly, full width.
 *
 * Three cards side by side made each one narrow enough that the numbers inside had to
 * shrink; one row that changes what is above it does the same job in a third of the space.
 */
@Composable
private fun PeriodToggle(period: ArenaPeriod, onChange: (ArenaPeriod) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ArenaColors.well).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ArenaPeriod.entries.forEach { p ->
            val on = p == period
            Text(
                stringResource(p.labelRes),
                fontFamily = Alias.body,
                fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (on) Alias.onAccent else ArenaColors.body,
                textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (on) Alias.accent else Color.Transparent)
                    .clickable { onChange(p) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

private fun number(value: Int): String = NumberFormat.getIntegerInstance().format(value)
