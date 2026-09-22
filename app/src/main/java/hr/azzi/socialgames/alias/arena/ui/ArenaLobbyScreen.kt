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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionMark
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
import hr.azzi.socialgames.alias.arena.ArenaBoardEntry
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaMode
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.arena.ArenaPhase
import hr.azzi.socialgames.alias.arena.ArenaRound
import hr.azzi.socialgames.alias.arena.arenaMMSS
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * Where a player waits for the next round to start.
 *
 * Android port of iOS `ArenaLobbyView.swift`.
 *
 * The lobby is the Arena's resting state, not a failure to get in. Rounds run on the
 * server whether anybody is watching or not, so arriving between them — or arriving too
 * late into one to be given a fair share of the clock — puts you here, and the screen is
 * written to read as "the next one is about to start" rather than as "you missed it".
 *
 * There is no ready toggle and no join button in the ordinary case. Staying here *is*
 * joining: the round takes whoever is present when it starts.
 */
@Composable
fun ArenaLobbyScreen(
    phase: ArenaPhase,
    round: ArenaRound?,
    mode: ArenaMode,
    languageLabel: String,
    languageFlag: String,
    playersInLobby: Int,
    board: List<ArenaBoardEntry>,
    forceJoinSeconds: Int?,
    onJoinNow: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onBack)
            CountdownHero(phase, round, playersInLobby)
            forceJoinSeconds?.let { JoinNowButton(it, onJoinNow) }
            SetupCard(mode, languageLabel, languageFlag, playersInLobby, board)
            AutoStartNotice(countdown(phase))
        }
    }
}

/** What the ring is counting down, which is not always the next round. */
private fun countdown(phase: ArenaPhase): Int = when (phase) {
    is ArenaPhase.Idle -> 0
    is ArenaPhase.Lobby -> phase.secondsToStart
    is ArenaPhase.Playing -> phase.secondsLeft
    is ArenaPhase.Results -> phase.secondsToNext
}

/**
 * The full length of whatever the ring is draining, so the arc starts full.
 *
 * The break is derived from the round's own cycle — next start minus this start, less the
 * play time — rather than hard-coded, because the server owns those numbers and changes
 * them without shipping an app.
 */
private fun ringTotal(phase: ArenaPhase, round: ArenaRound?): Int {
    val r = round ?: return maxOf(countdown(phase), 1)
    if (phase is ArenaPhase.Playing) return r.playSec
    val cycle = ((r.nextStartsAtMs - r.startsAtMs) / 1000).toInt()
    val brk = cycle - r.playSec
    return if (brk > 0) brk else maxOf(countdown(phase), 1)
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.ChevronLeft, null, tint = Color.White) }

        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.arena_lobby_eyebrow),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp,
                letterSpacing = 1.1.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1,
            )
            Text(
                stringResource(R.string.arena_lobby_title),
                fontFamily = Alias.display, fontSize = 24.sp, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            Modifier.clip(RoundedCornerShape(50)).background(ArenaPalette.onBrandPill)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(ArenaPalette.readyDot))
            Text(
                stringResource(R.string.arena_lobby_live),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
                color = Color.White, maxLines = 1,
            )
        }
    }
}

@Composable
private fun CountdownHero(phase: ArenaPhase, round: ArenaRound?, playersInLobby: Int) {
    val seconds = countdown(phase)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.10f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.padding(top = 20.dp), contentAlignment = Alignment.Center) {
            ArenaCountdownRing(seconds, ringTotal(phase, round), showsTime = false)
            // Sized to live inside the ring rather than across it. The caption is the
            // longest string here in several languages, so it gets the tighter budget.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(
                        if (phase is ArenaPhase.Playing) R.string.arena_lobby_round_ends
                        else R.string.arena_lobby_next_round
                    ),
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 9.sp,
                    letterSpacing = 0.6.sp, color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1, textAlign = TextAlign.Center,
                    modifier = Modifier.width(118.dp),
                )
                Text(
                    arenaMMSS(seconds),
                    fontFamily = Alias.display, fontSize = 32.sp, color = Color.White, maxLines = 1,
                )
            }
        }

        Row(
            Modifier.clip(RoundedCornerShape(50)).background(ArenaPalette.onBrandPill)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(ArenaPalette.readyDot))
            Text(
                stringResource(
                    when (phase) {
                        is ArenaPhase.Idle -> R.string.arena_lobby_connecting
                        is ArenaPhase.Playing -> R.string.arena_lobby_round_running
                        else -> R.string.arena_lobby_auto_join
                    }
                ),
                fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                color = Color.White, maxLines = 1,
            )
        }

        CountersCard(playersInLobby, Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp))
    }
}

/**
 * One number, not two.
 *
 * "Playing now" and "in the lobby" were the same room counted twice — and, with no
 * presence to read, both were the same estimate wearing different labels. A player
 * looking at them could only conclude that one of the two was wrong.
 */
@Composable
private fun CountersCard(playersInLobby: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ArenaColors.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "$playersInLobby",
            fontFamily = Alias.display, fontSize = 30.sp, color = Alias.accent, maxLines = 1,
        )
        Text(
            stringResource(R.string.arena_lobby_players_here),
            fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 2,
        )
    }
}

/**
 * Offered only between the automatic threshold and the point where a round stops being
 * one. The seconds are in the title because they are the whole decision.
 */
@Composable
private fun JoinNowButton(seconds: Int, onJoinNow: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Alias.accent)
            .clickable { onJoinNow(seconds) }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Bolt, null, tint = Alias.onAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.arena_lobby_join_now, seconds),
            fontFamily = Alias.display, fontSize = 15.sp, color = Alias.onAccent, maxLines = 1,
        )
    }
}

@Composable
private fun SetupCard(
    mode: ArenaMode,
    languageLabel: String,
    languageFlag: String,
    playersInLobby: Int,
    board: List<ArenaBoardEntry>,
) {
    ArenaCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(ArenaPalette.modeGlyph),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (mode == ArenaMode.GUESS) Icons.Filled.QuestionMark else Icons.Filled.Mic,
                    null, tint = Color.White, modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                ArenaSectionLabel(stringResource(R.string.arena_lobby_your_mode))
                Text(
                    stringResource(mode.titleRes),
                    fontFamily = Alias.display, fontSize = 20.sp, color = ArenaColors.title, maxLines = 1,
                )
                Text(
                    stringResource(mode.subtitleRes),
                    fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 2,
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(ArenaColors.well)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(languageFlag, fontSize = 13.sp)
                Text(
                    languageLabel,
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
                    color = Alias.blue600, maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(ArenaColors.border))
        Spacer(Modifier.height(14.dp))

        // The count lives in the hero above; repeating it here was the third appearance
        // of one number on one screen.
        ArenaSectionLabel(stringResource(R.string.arena_lobby_players_next))
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AvatarStrip(playersInLobby, board)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.arena_lobby_live_note),
                fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 2,
            )
        }
    }
}

/**
 * Faces to show in the strip.
 *
 * There is no roster endpoint — the server publishes counts, not a list of who is sitting
 * in the lobby — so these are the players from the most recent board, which in a room
 * that loops every couple of minutes is very nearly the same set of people. The "+N"
 * beside them is the real count, so the number is never a guess even when the faces are.
 */
@Composable
private fun AvatarStrip(playersInLobby: Int, board: List<ArenaBoardEntry>) {
    val ids = board.take(5).map { it.avatar }
    val overflow = maxOf(0, maxOf(playersInLobby, board.size) - ids.size)
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        ids.forEach { ArenaAvatarImage(it, 38.dp, ring = Color.White, ringWidth = 2.dp) }
        if (overflow > 0) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(ArenaColors.well),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+$overflow",
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
                    color = Alias.blue600, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AutoStartNotice(countdown: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Bolt, null, tint = Alias.accent, modifier = Modifier.size(20.dp))
        Column {
            Text(
                stringResource(R.string.arena_lobby_auto_title),
                fontFamily = Alias.display, fontSize = 14.sp, color = Color.White, maxLines = 1,
            )
            Text(
                stringResource(R.string.arena_lobby_auto_subtitle, countdown),
                fontFamily = Alias.body, fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f), maxLines = 2,
            )
        }
    }
}
