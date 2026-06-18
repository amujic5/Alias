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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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

@Composable
fun AiPlayDetailScreen(play: AIChallengePlay, challenge: AIChallenge, onClose: () -> Unit) {
    val rows = play.correctWords.map { it to true } + play.skippedWords.map { it to false }
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.ai_player_result, play.playerName), 22)
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("${challenge.deckName} · ${challenge.aiLanguage.aiName}",
                        color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 18.dp, vertical = 9.dp))
                }
                Spacer(Modifier.height(18.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(stringResource(R.string.ai_correct_label), play.correctWords.size, Alias.success, Modifier.weight(1f))
                    StatTile(stringResource(R.string.ai_skipped_label), play.skippedWords.size, Alias.textSecondary, Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))

                Overline(stringResource(R.string.ai_word_by_word))
                Spacer(Modifier.height(8.dp))

                CCard(Modifier.fillMaxWidth(), padding = 0) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        if (rows.isEmpty()) {
                            Text("—", color = Alias.textSecondary, fontFamily = Alias.display, fontSize = 24.sp,
                                modifier = Modifier.padding(24.dp))
                        } else {
                            rows.forEachIndexed { i, (word, ok) ->
                                WordRow(word, ok)
                                if (i != rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp)
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
private fun WordRow(word: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(if (ok) Alias.success else Color(0xFFC4CDDA)),
            contentAlignment = Alignment.Center) {
            Icon(if (ok) Icons.Filled.Check else Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(12.dp))
        Text(word, color = if (ok) Alias.textPrimary else Alias.textSecondary, fontFamily = Alias.display,
            fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (!ok) Text(stringResource(R.string.skipped), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = color, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
        Text(label, color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.5.sp)
    }
}
