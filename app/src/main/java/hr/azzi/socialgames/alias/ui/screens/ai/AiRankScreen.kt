package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import hr.azzi.socialgames.alias.ai.AIChallengePlay
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiRankScreen(
    challenge: AIChallenge,
    plays: List<AIChallengePlay>,
    myUid: String,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    val finished = plays.filter { it.finished }.sortedWith(compareByDescending<AIChallengePlay> { it.score }.thenBy { it.playedAt })
    val unfinished = plays.filter { !it.finished }

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.ai_leaderboard), 22)
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CCard(Modifier.fillMaxWidth()) {
                    Column {
                        Overline(stringResource(R.string.ai_score_to_beat), color = Alias.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(challenge.creatorName, Alias.blue600, 40)
                            Spacer(Modifier.size(12.dp))
                            Text(challenge.creatorName, color = Alias.textPrimary, fontFamily = Alias.display,
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Text("${challenge.creatorScore}", color = Alias.accent, fontFamily = Alias.display,
                                fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                        }
                    }
                }

                if (finished.isNotEmpty()) {
                    Overline(stringResource(R.string.ai_players))
                    CCard(Modifier.fillMaxWidth(), padding = 0) {
                        Column {
                            finished.forEachIndexed { i, p ->
                                RankRow(i + 1, p, challenge, myUid)
                                if (i != finished.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp)
                                    .padding(horizontal = 16.dp).background(Alias.divider))
                            }
                        }
                    }
                }

                if (unfinished.isNotEmpty()) {
                    Overline(stringResource(R.string.ai_didnt_finish))
                    CCard(Modifier.fillMaxWidth(), padding = 0) {
                        Column {
                            unfinished.forEach { p ->
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(p.playerName, Color(0xFF8A93A6), 36)
                                    Spacer(Modifier.size(12.dp))
                                    Text(p.playerName, color = Alias.textSecondary, fontFamily = Alias.display,
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                PillButton(stringResource(R.string.ai_share_challenge), PillKind.Primary, onClick = onShare)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, p: AIChallengePlay, challenge: AIChallenge, myUid: String) {
    val isCreator = p.playerId == challenge.creatorId
    val (badge, color) = when {
        isCreator -> stringResource(R.string.ai_badge_host) to Alias.textSecondary
        p.score > challenge.creatorScore -> stringResource(R.string.ai_badge_win) to Alias.success
        p.score == challenge.creatorScore -> stringResource(R.string.ai_badge_draw) to Color(0xFF8A93A6)
        else -> stringResource(R.string.ai_badge_lost) to Alias.danger
    }
    val me = p.playerId == myUid
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(Alias.textPrimary), contentAlignment = Alignment.Center) {
            Text("$rank", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(Modifier.size(12.dp))
        Text(p.playerName + (if (me) stringResource(R.string.ai_you_suffix) else ""), color = Alias.textPrimary, fontFamily = Alias.display,
            fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("${p.score}", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Spacer(Modifier.size(10.dp))
        Text(badge, color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
