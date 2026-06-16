package hr.azzi.socialgames.alias.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

data class TeamRow(val rank: Int, val name: String, val score: Int, val winner: Boolean)

data class ResultsUiState(
    val teamName: String,
    val correct: Int,
    val skip: Int,
    val rows: List<TeamRow>,
    val nextTeamName: String?,
    val explaining: String?,
    val answering: String?,
)

@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onEditAnswers: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            DisplayText(stringResource(R.string.results).uppercase(), 22,
                modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 10.dp))

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // This round card
                CCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Overline(stringResource(R.string.this_round), color = Alias.textSecondary)
                            Text(state.teamName, color = Alias.textPrimary, fontFamily = Alias.display,
                                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            Text("${state.correct} ${stringResource(R.string.corrected)} · ${state.skip} ${stringResource(R.string.skipped)}",
                                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                        }
                        Text(stringResource(R.string.edit_answers), color = Alias.blue600,
                            fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { onEditAnswers() }.padding(4.dp))
                    }
                }

                Column {
                    Overline(stringResource(R.string.game_stats))
                    Spacer(Modifier.height(8.dp))
                    CCard(Modifier.fillMaxWidth(), padding = 0) {
                        Column {
                            state.rows.forEachIndexed { i, row ->
                                ScoreRow(row)
                                if (i != state.rows.lastIndex)
                                    Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp).background(Alias.divider))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Bottom bar
            Column(
                Modifier.fillMaxWidth().shadow(20.dp).background(Color.White).padding(20.dp),
            ) {
                if (state.nextTeamName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Overline(stringResource(R.string.next_up), color = Alias.textSecondary)
                            Text(state.nextTeamName, color = Alias.textPrimary, fontFamily = Alias.display,
                                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        }
                        PillButton(stringResource(R.string.start), PillKind.Primary,
                            modifier = Modifier.padding(start = 12.dp), fillWidth = false, onClick = onStart)
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Alias.divider))
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        PlayerCol(stringResource(R.string.explaining), state.explaining ?: "", Modifier.weight(1f))
                        PlayerCol(stringResource(R.string.answering), state.answering ?: "", Modifier.weight(1f))
                    }
                } else {
                    PillButton(stringResource(R.string.finish), PillKind.Primary, onClick = onFinish)
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(row: TeamRow) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(50))
                .background(if (row.winner) Alias.accent else Alias.textPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text("${row.rank}", color = if (row.winner) Alias.onAccent else Color.White,
                fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name + if (row.winner) "  👑" else "", color = Alias.textPrimary,
                fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("${stringResource(R.string.score)} ${row.score}", color = Alias.textSecondary,
                fontFamily = Alias.body, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlayerCol(label: String, name: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Overline(label, color = Alias.textSecondary)
        Text(name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
