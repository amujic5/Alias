package hr.azzi.socialgames.alias.ui.screens.ai

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
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
import hr.azzi.socialgames.alias.ai.AIChallenge
import hr.azzi.socialgames.alias.ai.AIChallengeOutcome
import hr.azzi.socialgames.alias.ai.AIUserStats
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiHubScreen(
    isSignedIn: Boolean,
    username: String,
    uid: String,
    stats: AIUserStats,
    recent: List<AIChallenge>,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onPractice: () -> Unit,
    onChallenge: () -> Unit,
    onProfile: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenChallenge: (AIChallenge) -> Unit,
    /**
     * The Arena, when it is on offer.
     *
     * Null takes the door away without shipping an app — the Arena signs a newcomer in
     * anonymously and lets them play a round first, so unlike Challenge it is not gated
     * behind signing in.
     */
    onArena: (() -> Unit)? = null,
    /** The Arena's side of the player: the level, the XP and the face. */
    arenaProfile: hr.azzi.socialgames.alias.arena.ArenaProfileStore? = null,
    onLeaderboard: (() -> Unit)? = null,
) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 14.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Spacer(Modifier.width(10.dp))
                Overline(stringResource(R.string.ai_hub_title))
            }
            Spacer(Modifier.height(8.dp))

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // A guest gets the way in, not a profile: theirs is a generated name and
                // nothing to manage, and the profile would only send them back to sign in.
                if (isSignedIn) {
                    ProfileHeader(username, arenaProfile, onProfile)
                } else {
                    JoinCard(onSignIn)
                }

                // Above the two mode cards, and bigger than them: those are things you
                // can do, this is the thing the app wants you to do.
                onArena?.let { hr.azzi.socialgames.alias.arena.ui.ArenaEntryCard(onClick = it) }

                if (arenaProfile != null && onLeaderboard != null) {
                    LeaderboardRow(arenaProfile, onLeaderboard)
                }

                ActionCard("AI", Color(0xFFB9A7F6), stringResource(R.string.ai_practice_title),
                    stringResource(R.string.ai_practice_subtitle), locked = false, onClick = onPractice)

                ActionCard("VS", Color(0xFFF7A7BC), stringResource(R.string.ai_challenge_title),
                    stringResource(R.string.ai_challenge_subtitle), locked = !isSignedIn,
                    onClick = if (isSignedIn) onChallenge else onSignIn)

                if (isSignedIn && recent.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Overline(stringResource(R.string.ai_recent_challenges))
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(R.string.ai_see_all), color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, modifier = Modifier.clickable { onSeeAll() })
                    }
                    CCard(Modifier.fillMaxWidth(), padding = 0) {
                        Column {
                            recent.forEachIndexed { i, ch ->
                                RecentRow(ch, uid) { onOpenChallenge(ch) }
                                if (i != recent.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp)
                                    .padding(horizontal = 16.dp).background(Alias.divider))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    arena: hr.azzi.socialgames.alias.arena.ArenaProfileStore?,
    onClick: () -> Unit,
) {
    val summary = arena?.summary?.collectAsState()?.value
    val avatar = arena?.avatar?.collectAsState()?.value

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(hr.azzi.socialgames.alias.arena.ArenaColors.card)
            .clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        hr.azzi.socialgames.alias.arena.ui.ArenaAvatarImage(
            avatar, 68.dp, ring = Alias.accent, ringWidth = 3.dp,
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "@$username",
                color = hr.azzi.socialgames.alias.arena.ArenaColors.title,
                fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                maxLines = 1,
            )
            Text(
                stringResource(R.string.ai_hub_level_pill, summary?.level ?: 1),
                color = Color.White, fontFamily = Alias.body,
                fontWeight = FontWeight.Black, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF3F4BA8))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
            Text(
                stringResource(
                    R.string.ai_hub_xp_progress,
                    summary?.xpIntoLevel ?: 0, summary?.xpForLevel ?: 1,
                ),
                color = hr.azzi.socialgames.alias.arena.ArenaColors.title,
                fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                maxLines = 1,
            )
            XpBar(summary?.levelProgress ?: 0f)
            Text(
                stringResource(R.string.ai_hub_xp_hint),
                color = hr.azzi.socialgames.alias.arena.ArenaColors.body,
                fontFamily = Alias.body, fontSize = 12.sp, maxLines = 1,
            )
        }

        Icon(
            Icons.Filled.ChevronRight, null,
            tint = hr.azzi.socialgames.alias.arena.ArenaColors.body,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Progress to the next level, with the head of the bar marked.
 *
 * The dot is not decoration: at the start of a level the fill is a few pixels wide and
 * reads as an empty track, which says "you have nothing" to somebody who has just earned
 * their first XP.
 */
@Composable
private fun XpBar(progress: Float) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().height(14.dp),
    ) {
        val width = maxWidth
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50))
            .background(hr.azzi.socialgames.alias.arena.ArenaColors.well))
        Box(
            Modifier.width(maxOf(10.dp, width * progress)).height(14.dp)
                .clip(RoundedCornerShape(50)).background(Alias.accent),
        )
        Box(
            Modifier.offset(x = maxOf(0.dp, width * progress - 14.dp))
                .size(14.dp).clip(CircleShape).background(Alias.accent),
        )
    }
}

/**
 * Where you stand in the Arena, and a way into the full board.
 *
 * Reads the weekly standing because that is what the standings screen opens on — a row
 * that said one thing and then pushed a screen saying another would be worse than no row.
 */
@Composable
private fun LeaderboardRow(
    arena: hr.azzi.socialgames.alias.arena.ArenaProfileStore,
    onClick: () -> Unit,
) {
    val weekly = arena.weekly.collectAsState().value

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(hr.azzi.socialgames.alias.arena.ArenaColors.card)
            .clickable { onClick() }.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(Alias.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                weekly.rank?.let { "#$it" } ?: "#—",
                color = Alias.onAccent, fontFamily = Alias.display,
                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.ai_hub_leaderboard_title),
                color = hr.azzi.socialgames.alias.arena.ArenaColors.title,
                fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp,
                maxLines = 1,
            )
            Text(
                weekly.rank?.let { r ->
                    weekly.score?.let { stringResource(R.string.ai_hub_leaderboard_you, r, it) }
                } ?: stringResource(R.string.ai_hub_leaderboard_empty),
                color = hr.azzi.socialgames.alias.arena.ArenaColors.body,
                fontFamily = Alias.body, fontSize = 13.sp, maxLines = 1,
            )
        }
        Icon(
            Icons.Filled.ChevronRight, null,
            tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Text(text, color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun JoinCard(onSignIn: () -> Unit) {
    CCard(Modifier.fillMaxWidth()) {
        Column {
            Text(stringResource(R.string.ai_join_title), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.ai_join_subtitle),
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            PillButton(stringResource(R.string.ai_sign_in_google), PillKind.Dark, onClick = onSignIn)
        }
    }
}

@Composable
private fun ActionCard(badge: String, badgeColor: Color, title: String, subtitle: String, locked: Boolean, onClick: () -> Unit) {
    CCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(badgeColor), contentAlignment = Alignment.Center) {
                Text(badge, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(subtitle, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
            }
            if (locked) Icon(Icons.Filled.Lock, null, tint = Alias.textSecondary, modifier = Modifier.size(22.dp))
            else Icon(Icons.Filled.ChevronRight, null, tint = Alias.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RecentRow(ch: AIChallenge, uid: String, onClick: () -> Unit) {
    val amCreator = ch.creatorId == uid
    val label = if (amCreator) stringResource(R.string.ai_your_challenge) else stringResource(R.string.ai_vs_name, ch.creatorName)
    val (text, color) = when {
        amCreator -> stringResource(R.string.ai_players_count, ch.players.size) to Alias.textSecondary
        ch.didNotFinish(uid) -> stringResource(R.string.ai_didnt_finish) to Alias.textSecondary
        else -> when (ch.outcome(uid)) {
            AIChallengeOutcome.WIN -> stringResource(R.string.ai_won_score, ch.score(uid), ch.creatorScore) to Alias.success
            AIChallengeOutcome.DRAW -> stringResource(R.string.ai_draw_score, ch.score(uid), ch.creatorScore) to Alias.textSecondary
            else -> stringResource(R.string.ai_lost_score, ch.score(uid), ch.creatorScore) to Alias.danger
        }
    }
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(if (amCreator) "You" else ch.creatorName, Alias.blue600, 40)
        Spacer(Modifier.size(12.dp))
        Text(label, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(text, color = color, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun Avatar(name: String, color: Color, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" }, color = Color.White,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = (sizeDp * 0.4).sp)
    }
}
