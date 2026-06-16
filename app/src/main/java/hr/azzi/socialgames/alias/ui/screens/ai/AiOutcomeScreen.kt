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
import androidx.compose.foundation.shape.CircleShape
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
import hr.azzi.socialgames.alias.ai.AIChallengeOutcome
import hr.azzi.socialgames.alias.ai.AIUserStats
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiOutcomeScreen(
    challenge: AIChallenge,
    myName: String,
    myScore: Int,
    myTotal: Int,
    stats: AIUserStats,
    onShare: () -> Unit,
    onSeeBoard: () -> Unit,
    onClose: () -> Unit,
) {
    val outcome = when {
        myScore > challenge.creatorScore -> AIChallengeOutcome.WIN
        myScore == challenge.creatorScore -> AIChallengeOutcome.DRAW
        else -> AIChallengeOutcome.LOST
    }
    val (badge, badgeColor, title) = when (outcome) {
        AIChallengeOutcome.WIN -> Triple("W", Alias.success, stringResource(R.string.ai_you_won))
        AIChallengeOutcome.DRAW -> Triple("D", Color(0xFF8A93A6), stringResource(R.string.ai_its_draw))
        else -> Triple("L", Alias.danger, stringResource(R.string.ai_you_lost))
    }
    val record = stringResource(R.string.ai_record, stats.wins, stats.losses, stats.draws)

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(30.dp))
            Box(Modifier.size(110.dp).clip(CircleShape).background(badgeColor), contentAlignment = Alignment.Center) {
                Text(badge, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 46.sp)
            }
            Spacer(Modifier.height(18.dp))
            DisplayText(title, 34)
            Spacer(Modifier.height(6.dp))
            Text(record, color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 15.sp)
            Spacer(Modifier.height(28.dp))

            CCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Side(myName.ifBlank { "You" }, Alias.accent, myScore, "$myScore/$myTotal", true, Modifier.weight(1f))
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Alias.textPrimary), contentAlignment = Alignment.Center) {
                        Text("VS", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                    Side(challenge.creatorName, Alias.blue600, challenge.creatorScore, "${challenge.creatorScore}", false, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.weight(1f))
            PillButton(stringResource(R.string.ai_see_full_board), PillKind.Primary, onClick = onSeeBoard)
            Spacer(Modifier.height(12.dp))
            PillButton(stringResource(R.string.ai_share_result), PillKind.Light, onClick = onShare)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.ai_close), color = Color.White.copy(alpha = 0.9f), fontFamily = Alias.body, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, modifier = Modifier.clickable { onClose() }.padding(8.dp))
        }
    }
}

@Composable
private fun Side(name: String, color: Color, score: Int, sub: String, highlight: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(name, color, 56)
        Spacer(Modifier.height(8.dp))
        Text(name, color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("$score", color = if (highlight) Alias.textPrimary else Alias.textSecondary,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp)
        Text(sub, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 12.sp)
    }
}
