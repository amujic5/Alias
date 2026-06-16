package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.ai.AIChallenge
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiIntroScreen(challenge: AIChallenge, onPlay: () -> Unit, onClose: () -> Unit) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            Avatar(challenge.creatorName, Alias.accent, 96)
            Spacer(Modifier.height(20.dp))
            DisplayText("${challenge.creatorName} challenges you!", 28)
            Spacer(Modifier.height(28.dp))
            CCard(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(challenge.deckName, color = Alias.textPrimary, fontFamily = Alias.display,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, fontSize = 20.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("${challenge.words.size} words · ${challenge.totalSeconds}s · ${challenge.aiLanguage.display}",
                        color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text("Beat their score of ${challenge.creatorScore}", color = Alias.blue600,
                        fontFamily = Alias.display, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            PillButton("Play challenge", PillKind.Primary, onClick = onPlay)
            Spacer(Modifier.height(12.dp))
            PillButton("Not now", PillKind.Light, onClick = onClose)
        }
    }
}
