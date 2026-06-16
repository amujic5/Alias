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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import hr.azzi.socialgames.alias.ai.AIPracticeResult
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiResultScreen(
    result: AIPracticeResult,
    title: String,
    onPlayAgain: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val rows = result.correctWords.map { it to true } + result.skippedWords.map { it to false }
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            Overline(title)
            Spacer(Modifier.height(6.dp))
            DisplayText(stringResource(R.string.ai_round_complete), 30)
            Spacer(Modifier.height(4.dp))
            Text("${result.deckName} · ${result.language}", color = Color.White.copy(alpha = 0.85f),
                fontFamily = Alias.body, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.ai_correct_label), result.correct, Alias.success, Modifier.weight(1f))
                StatTile(stringResource(R.string.ai_skipped_label), result.skipped, Alias.textSecondary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            CCard(Modifier.weight(1f).fillMaxWidth(), padding = 0) {
                LazyColumn(Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                    items(rows) { (word, ok) ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (ok) "✓" else "✕", color = if (ok) Alias.success else Alias.textSecondary,
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(word, color = Alias.textPrimary, fontFamily = Alias.display,
                                fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (!ok) Text("skipped", color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (onPlayAgain != null) {
                PillButton(stringResource(R.string.ai_play_again), PillKind.Primary, onClick = onPlayAgain)
                Spacer(Modifier.height(12.dp))
            }
            PillButton(stringResource(R.string.ai_close_caps), PillKind.Light, onClick = onClose)
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", color = color, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
        Text(label, color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.5.sp)
    }
}
