package hr.azzi.socialgames.alias.arena.ui

import hr.azzi.socialgames.alias.ui.theme.BackButton
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import hr.azzi.socialgames.alias.ai.AIChallenge
import hr.azzi.socialgames.alias.ai.AIUserStats
import hr.azzi.socialgames.alias.arena.ArenaAnalytics
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaProfile
import hr.azzi.socialgames.alias.arena.ArenaProfileStore
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.PillButton

/**
 * One profile, whichever door you came through.
 *
 * Android port of iOS `PlayerProfileView.swift`.
 *
 * The AI section and the Arena each used to describe the same person on their own screen,
 * and the two disagreed — about the avatar, most visibly, because one drew a letter and
 * the other drew the chosen face. This is the single screen both open.
 *
 * Drawn on the Arena backdrop rather than the app's brand gradient, and adaptive with it:
 * a profile is something a player opens deliberately and reads, not a branded moment.
 */
@Composable
fun ArenaPlayerProfileScreen(
    profile: ArenaProfileStore,
    stats: AIUserStats,
    recent: List<AIChallenge>,
    isAnonymous: Boolean,
    onPickAvatar: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenStandings: () -> Unit,
    onOpenChallenge: (AIChallenge) -> Unit,
    onSeeAll: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
) {
    val name by profile.name.collectAsState()

    LaunchedEffect(Unit) {
        ArenaAnalytics.profileShown()
        // First visit only: the store answers a repeat call in a line. Re-reading a
        // profile because somebody looked at it again is a page of queries to learn
        // nothing.
        profile.refreshIfNeeded()
    }

    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header(isAnonymous, onSignOut, onClose)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArenaProfileIdentity(profile, onPickAvatar, onOpenRewards)
                Text(
                    "@${name.ifEmpty { stringResource(R.string.arena_profile_title) }}",
                    fontFamily = Alias.display, fontSize = 28.sp, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                ArenaProfileRanking(profile, onOpenStandings)
                MatchRecord(stats)
                RecentSection(recent, onOpenChallenge, onSeeAll)
                if (isAnonymous) GuestCard(onSignIn)
            }
        }
    }
}

@Composable
private fun Header(isAnonymous: Boolean, onSignOut: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClose)
        Spacer(Modifier.size(12.dp))
        Text(
            stringResource(R.string.arena_profile_title),
            fontFamily = Alias.display, fontSize = 22.sp, color = Color.White,
            maxLines = 1, modifier = Modifier.weight(1f),
        )
        // Nothing to sign out of as a guest, and offering it would suggest there is.
        if (!isAnonymous) {
            Text(
                stringResource(R.string.arena_profile_sign_out),
                fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f), maxLines = 1,
                modifier = Modifier.clickable(onClick = onSignOut).padding(6.dp),
            )
        }
    }
}

/** Wins, losses and draws — the Challenge record, which is not an Arena number. */
@Composable
private fun MatchRecord(stats: AIUserStats) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ArenaSectionLabel(stringResource(R.string.arena_profile_match_record), Color.White.copy(alpha = 0.7f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordTile(stringResource(R.string.ai_wins), stats.wins, Alias.success, Modifier.weight(1f))
            RecordTile(stringResource(R.string.ai_losses), stats.losses, Alias.danger, Modifier.weight(1f))
            RecordTile(stringResource(R.string.ai_draws), stats.draws, ArenaColors.body, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecordTile(label: String, value: Int, tint: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(ArenaColors.card).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", fontFamily = Alias.display, fontSize = 30.sp, color = tint, maxLines = 1)
        Text(
            label.uppercase(),
            fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 10.sp,
            letterSpacing = 1.sp, color = ArenaColors.body, maxLines = 1,
        )
    }
}

@Composable
private fun RecentSection(
    recent: List<AIChallenge>,
    onOpen: (AIChallenge) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ArenaSectionLabel(
                stringResource(R.string.ai_recent_challenges),
                Color.White.copy(alpha = 0.7f),
                Modifier.weight(1f),
            )
            // The hub used to carry this link. It shows five and the full list holds
            // everything, so dropping the hub's copy would have left the list screen with
            // no door anywhere in the app.
            if (recent.isNotEmpty()) {
                Text(
                    stringResource(R.string.ai_see_all),
                    fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = Alias.accent, maxLines = 1,
                    modifier = Modifier.clickable(onClick = onSeeAll).padding(4.dp),
                )
            }
        }

        ArenaCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            if (recent.isEmpty()) {
                Text(
                    stringResource(R.string.ai_profile_empty),
                    fontFamily = Alias.body, fontSize = 14.sp, color = ArenaColors.body,
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                )
            } else {
                recent.forEachIndexed { index, challenge ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(challenge) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            challenge.creatorName,
                            fontFamily = Alias.display, fontSize = 15.sp, color = ArenaColors.title,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text(
                            challenge.deckName,
                            fontFamily = Alias.body, fontSize = 12.sp, color = ArenaColors.body, maxLines = 1,
                        )
                    }
                    if (index != recent.lastIndex) ArenaRowDivider()
                }
            }
        }
    }
}

/**
 * The one thing a guest is told, and only once they are looking at their own profile.
 *
 * Not a gate: the Arena works without an account, and everything on this screen above it
 * is real. What signing in buys is that it survives the phone.
 */
@Composable
private fun GuestCard(onSignIn: () -> Unit) {
    ArenaCard(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.arena_profile_guest_title),
            fontFamily = Alias.display, fontSize = 21.sp, color = ArenaColors.title,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.arena_profile_guest_body),
            fontFamily = Alias.body, fontSize = 14.sp, color = ArenaColors.body,
        )
        Spacer(Modifier.height(12.dp))
        PillButton(title = stringResource(R.string.ai_sign_in_google), onClick = onSignIn)
    }
}
