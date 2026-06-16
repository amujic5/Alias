package hr.azzi.socialgames.alias.ui.screens.ai

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                Overline("AI CHALLENGE")
            }
            Spacer(Modifier.height(8.dp))

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isSignedIn) ProfileHeader(username, stats, onProfile) else JoinCard(onSignIn)

                ActionCard("AI", Color(0xFFB9A7F6), "Practice with AI",
                    "Warm up — no stakes, no login", locked = false, onClick = onPractice)

                ActionCard("VS", Color(0xFFF7A7BC), "Challenge a Friend",
                    "Beat a friend · counts on your record", locked = !isSignedIn,
                    onClick = if (isSignedIn) onChallenge else onSignIn)

                if (isSignedIn && recent.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Overline("RECENT CHALLENGES")
                        Spacer(Modifier.weight(1f))
                        Text("See all", color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
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
private fun ProfileHeader(username: String, stats: AIUserStats, onClick: () -> Unit) {
    val level = 1 + stats.wins / 5
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.16f))
        .clickable { onClick() }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(username, Alias.accent, 52)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("@${username}", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatChip("${stats.wins} W", Alias.success)
                    StatChip("${stats.losses} L", Alias.danger)
                    StatChip("${stats.draws} D", Color.White.copy(alpha = 0.3f))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LV", color = Color.White.copy(alpha = 0.7f), fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("$level", color = Alias.accent, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }
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
            Text("Join the arena", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text("Sign in to challenge friends and keep your win record.",
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            PillButton("Sign in with Google", PillKind.Dark, onClick = onSignIn)
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
    val label = if (amCreator) "Your challenge" else "vs ${ch.creatorName}"
    val (text, color) = when {
        amCreator -> "${ch.players.size} players" to Alias.textSecondary
        ch.didNotFinish(uid) -> "DIDN'T FINISH" to Alias.textSecondary
        else -> when (ch.outcome(uid)) {
            AIChallengeOutcome.WIN -> "Won ${ch.score(uid)}–${ch.creatorScore}" to Alias.success
            AIChallengeOutcome.DRAW -> "Draw ${ch.score(uid)}–${ch.creatorScore}" to Alias.textSecondary
            else -> "Lost ${ch.score(uid)}–${ch.creatorScore}" to Alias.danger
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
