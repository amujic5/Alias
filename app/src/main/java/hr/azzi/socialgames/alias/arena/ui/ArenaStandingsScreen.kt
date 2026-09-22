package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.ArenaAnalytics
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.arena.ArenaPeriod
import hr.azzi.socialgames.alias.arena.ArenaProfileStore
import hr.azzi.socialgames.alias.arena.ArenaStandingRow
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * Who is ahead — today, this week, this month.
 *
 * Android port of iOS `ArenaStandingsView.swift`.
 *
 * One table, for everybody. Rounds are scored on `net` — guessed minus skipped — and that
 * number does not depend on who else was in the room, so there is nothing to separate
 * out. Splitting it by country would be several tables where there is one competition.
 */
@Composable
fun ArenaStandingsScreen(profile: ArenaProfileStore, onBack: () -> Unit) {
    val mode by profile.mode.collectAsState()
    var period by remember { mutableStateOf(ArenaPeriod.WEEKLY) }

    val rows = remember { mutableStateListOf<ArenaStandingRow>() }
    var mine by remember { mutableStateOf<ArenaStandingRow?>(null) }
    var loading by remember { mutableStateOf(true) }
    var cursor by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var loadingMore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { ArenaAnalytics.leaderboardShown() }

    // Keyed on both controls, so switching either cancels the read still in flight for
    // the old one instead of letting two land out of order.
    LaunchedEffect(period, mode) {
        loading = true
        cursor = null
        val page = profile.standings(period)
        rows.clear()
        rows.addAll(page.rows)
        cursor = page.next
        mine = profile.myStanding(period)
        loading = false
    }

    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        Column(Modifier.fillMaxSize()) {
            ArenaScreenHeader(stringResource(R.string.arena_standings_title), onBack)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            ArenaModeSwitcher(mode, onChange = { profile.setMode(it) })
            PeriodPicker(period) { period = it }

            when {
                loading && rows.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                rows.isEmpty() -> EmptyState()
                else -> {
                    // Fetch the next page two rows from the bottom, so it is usually
                    // there before the scroll reaches it.
                    OnLastVisible(listState) { last ->
                        if (!loadingMore && cursor != null && last >= rows.size - 2) {
                            loadingMore = true
                            val page = profile.standings(
                                period = period,
                                after = cursor,
                                rankOffset = rows.size,
                                // A tie that straddles the page boundary has to keep its
                                // rank across it, and only this side knows what the
                                // bottom of the page above scored.
                                previous = rows.lastOrNull(),
                            )
                            rows.addAll(page.rows)
                            cursor = page.next
                            loadingMore = false
                        }
                    }
                    StandingsList(rows, mine, listState)
                }
            }
            }
        }
    }
}

/**
 * Runs [block] whenever the last visible index changes.
 *
 * Extracted so the pagination reads as one thought rather than as three nested state
 * observations inside the layout.
 */
@Composable
private fun OnLastVisible(
    listState: androidx.compose.foundation.lazy.LazyListState,
    block: suspend (Int) -> Unit,
) {
    val last by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(last) { block(last) }
}

@Composable
private fun PeriodPicker(period: ArenaPeriod, onChange: (ArenaPeriod) -> Unit) {
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
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Alias.accent else Color.Transparent)
                    .clickable { onChange(p) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StandingsList(
    rows: List<ArenaStandingRow>,
    mine: ArenaStandingRow?,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val myIndex = remember(rows.size) { rows.indexOfFirst { it.isMe }.takeIf { it >= 0 } }

    /**
     * A leaderboard whose whole point is "where am I" should never make you hunt for
     * yourself. If your row is on screen it speaks for itself; if it is off the top or
     * the bottom, it is held against that edge instead.
     */
    val myPosition by remember {
        derivedStateOf {
            if (mine == null) return@derivedStateOf StandingPosition.ABSENT
            val index = myIndex ?: return@derivedStateOf StandingPosition.BELOW
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf StandingPosition.BELOW
            when {
                index in visible.first().index..visible.last().index -> StandingPosition.VISIBLE
                index < visible.first().index -> StandingPosition.ABOVE
                else -> StandingPosition.BELOW
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(ArenaColors.card)
                .border(1.dp, ArenaColors.border, RoundedCornerShape(22.dp)),
        ) {
            itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                StandingRowView(row)
                if (index < rows.size - 1) ArenaRowDivider()
            }
        }

        when (myPosition) {
            StandingPosition.ABOVE -> mine?.let { PinnedStanding(it, Modifier.align(Alignment.TopCenter)) }
            StandingPosition.BELOW -> mine?.let { PinnedStanding(it, Modifier.align(Alignment.BottomCenter)) }
            // For somebody who has not played this period: they have no row to pin, and
            // an empty bottom edge would read as the board simply not knowing about them.
            StandingPosition.ABSENT -> NotPlayedNotice(Modifier.align(Alignment.BottomCenter))
            StandingPosition.VISIBLE -> Unit
        }
    }
}

private enum class StandingPosition { VISIBLE, ABOVE, BELOW, ABSENT }

@Composable
private fun PinnedStanding(row: ArenaStandingRow, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().padding(4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ArenaColors.card)
            .border(2.dp, Alias.accent, RoundedCornerShape(22.dp)),
    ) { StandingRowView(row) }
}

@Composable
private fun NotPlayedNotice(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().padding(4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ArenaColors.card)
            .border(1.dp, ArenaColors.border, RoundedCornerShape(22.dp)),
    ) {
        Text(
            stringResource(R.string.arena_standings_not_played),
            fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = ArenaColors.body, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun StandingRowView(row: ArenaStandingRow) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (row.isMe) ArenaPalette.youRow else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${row.rank}",
            fontFamily = Alias.body,
            fontWeight = if (row.isMe) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (row.rank <= 3) Alias.accent else ArenaColors.body,
            maxLines = 1, modifier = Modifier.width(26.dp),
        )
        ArenaAvatarImage(row.avatar, 36.dp, ring = null)
        Text(
            if (row.isMe) stringResource(R.string.arena_results_you) else row.name,
            fontFamily = Alias.display, fontSize = 15.sp, color = ArenaColors.title,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        // The board's own sort key: the sum of this player's best rounds for this period,
        // times five. A whole number, because it is one.
        Text(
            "${row.score}",
            fontFamily = Alias.display, fontSize = 16.sp,
            color = if (row.isMe) Alias.accent else ArenaColors.body, maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.arena_standings_empty),
            fontFamily = Alias.display, fontSize = 20.sp, color = ArenaColors.title,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            stringResource(R.string.arena_standings_empty_hint),
            fontFamily = Alias.body, fontSize = 13.sp, color = ArenaColors.body,
            textAlign = TextAlign.Center,
        )
    }
}

val ArenaPeriod.labelRes: Int
    get() = when (this) {
        ArenaPeriod.DAILY -> R.string.arena_profile_daily
        ArenaPeriod.WEEKLY -> R.string.arena_profile_weekly
        ArenaPeriod.MONTHLY -> R.string.arena_profile_monthly
    }
