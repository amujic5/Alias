package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import hr.azzi.socialgames.alias.arena.ArenaBoardEntry
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.arena.arenaMMSS
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.PillButton

/**
 * The round's standings, shown for the length of the break and then replaced by the lobby
 * when the next one starts.
 *
 * Android port of iOS `ArenaResultsView.swift`.
 *
 * One list, all of it. The screen used to open with a headline, a countdown, a badge row
 * and a three-avatar podium before the table began — most of a phone spent on the three
 * players a round has, and the player themself somewhere underneath it. The top three are
 * rows one, two and three now, and what they get instead of a stage is a coloured rank
 * and a ring, which costs nothing vertically.
 */
@Composable
fun ArenaResultsScreen(
    board: List<ArenaBoardEntry>,
    boardLoaded: Boolean,
    secondsToNext: Int,
    secondsToResults: Int,
    waitTotal: Int,
    xpGained: Int?,
    onStay: () -> Unit,
    onLeave: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusLine(secondsToNext, xpGained)
            when {
                !boardLoaded -> Waiting(secondsToResults, waitTotal)
                board.isEmpty() -> EmptyCard()
                else -> Board(board)
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
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLeave).padding(8.dp),
            )
        }
    }
}

/**
 * What the removed header was for, on one line: when the next round starts, and what this
 * one paid. The XP is shown here rather than on the round review because this is when the
 * scorer has actually awarded it — the same pass that built the board below.
 */
@Composable
private fun StatusLine(secondsToNext: Int, xpGained: Int?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Schedule, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.arena_results_next_in, arenaMMSS(secondsToNext)),
            fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 14.sp,
            color = Color.White, maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        if (xpGained != null) {
            Text(
                stringResource(R.string.arena_review_xp, xpGained),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
                color = Color(0xFF6B4E00), maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Alias.accent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Board(board: List<ArenaBoardEntry>) {
    val listState = rememberLazyListState()
    val myIndex = remember(board) { board.indexOfFirst { it.isMe }.takeIf { it >= 0 } }

    // Where the player is relative to what they can see. A leaderboard whose whole point
    // is "where am I" should never make you hunt for yourself.
    val myPosition by remember {
        derivedStateOf {
            val index = myIndex ?: return@derivedStateOf MyRow.ABSENT
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf MyRow.BELOW
            when {
                index in visible.first().index..visible.last().index -> MyRow.VISIBLE
                index < visible.first().index -> MyRow.ABOVE
                else -> MyRow.BELOW
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(ArenaColors.card)
                .border(1.dp, ArenaColors.border, RoundedCornerShape(22.dp)),
        ) {
            itemsIndexed(board, key = { _, row -> row.id }) { index, row ->
                BoardRow(row)
                if (index < board.size - 1) ArenaRowDivider()
            }
        }

        val mine = myIndex?.let { board[it] }
        if (mine != null && myPosition == MyRow.ABOVE) {
            PinnedRow(mine, Modifier.align(Alignment.TopCenter))
        }
        if (mine != null && myPosition == MyRow.BELOW) {
            PinnedRow(mine, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private enum class MyRow { VISIBLE, ABOVE, BELOW, ABSENT }

/** The player's own row, held against whichever edge it has scrolled past. */
@Composable
private fun PinnedRow(row: ArenaBoardEntry, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ArenaColors.card)
            .border(2.dp, Alias.accent, RoundedCornerShape(22.dp)),
    ) { BoardRow(row) }
}

/** Gold, lilac, bronze for the first three — the podium's colours, kept where they cost
 *  one line each instead of a third of the screen. */
private fun tint(rank: Int): Color? = ArenaPalette.podium.getOrNull(rank - 1)

@Composable
private fun BoardRow(row: ArenaBoardEntry) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (row.isMe) ArenaPalette.youRow else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${row.rank}",
            fontFamily = Alias.body,
            fontWeight = if (row.isMe || row.rank <= 3) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 15.sp,
            color = tint(row.rank) ?: ArenaColors.body,
            maxLines = 1,
            modifier = Modifier.width(26.dp),
        )
        ArenaAvatarImage(row.avatar, 36.dp, ring = tint(row.rank), ringWidth = if (row.rank == 1) 3.dp else 2.dp)
        Text(
            if (row.isMe) stringResource(R.string.arena_results_you) else row.name,
            fontFamily = Alias.display, fontSize = 16.sp, color = ArenaColors.title,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(
            "${row.net}",
            fontFamily = Alias.display, fontSize = 17.sp,
            color = if (row.isMe) Alias.accent else ArenaColors.body, maxLines = 1,
        )
    }
}

/** Submissions closing, then the server scoring: a short, known wait. */
@Composable
private fun Waiting(secondsToResults: Int, waitTotal: Int) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ArenaCountdownRing(secondsToResults, maxOf(waitTotal, 1))
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.arena_results_scoring),
            fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center,
        )
    }
}

/**
 * A round nobody submitted to. Possible at quiet hours before the bots top the room up,
 * and much better than an empty card with no explanation.
 */
@Composable
private fun EmptyCard() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ArenaCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.arena_results_empty),
                fontFamily = Alias.body, fontSize = 14.sp, color = ArenaColors.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            )
        }
    }
}
