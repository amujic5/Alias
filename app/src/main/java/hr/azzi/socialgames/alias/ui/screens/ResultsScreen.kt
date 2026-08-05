package hr.azzi.socialgames.alias.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.Analytics
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundCardInfo
import hr.azzi.socialgames.alias.Service.RoundVideo.shareRoundImage
import hr.azzi.socialgames.alias.Service.RoundVideo.writeRoundShareImage
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind
import kotlinx.coroutines.launch

data class TeamRow(val rank: Int, val name: String, val score: Int, val winner: Boolean)

data class ResultsUiState(
    val teamName: String,
    val correct: Int,
    val skip: Int,
    val rows: List<TeamRow>,
    val nextTeamName: String?,
    val explaining: String?,
    val answering: String?,
    /** The round video, if one was filmed and came out. */
    val replayPath: String? = null,
    /** Everything the shareable result image draws — the round that just ended. */
    val shareInfo: RoundCardInfo? = null,
)

@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onEditAnswers: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    /** Leaving the game from here. Asked for first — a round has been played by now. */
    onQuit: () -> Unit = {},
    /** Whether this device can film at all; the row is not offered when it cannot. */
    captureMomentAvailable: Boolean = false,
    /** Carried over from the round just played, so the usual answer needs no tap. */
    captureMoment: Boolean = false,
    onCaptureMomentChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf<String?>(null) }
    var showQuit by remember { mutableStateOf(false) }
    var showCaptureInfo by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }

    // Back is the only way out of this screen, and there is a game in progress behind it.
    BackHandler { showQuit = true }

    playing?.let { path -> ReplayPlayer(path) { playing = null } }
    if (showQuit) {
        QuitGameDialog(onConfirm = onQuit, onDismiss = { showQuit = false })
    }
    if (showCaptureInfo) {
        DualCameraSheet(
            isOn = captureMoment,
            onToggle = onCaptureMomentChange,
            onDismiss = { showCaptureInfo = false },
        )
    }

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
                    Column {
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
                        state.shareInfo?.let { info ->
                            Spacer(Modifier.height(12.dp))
                            SharePill(
                                // Rendering is quick but not free; the pill says so rather
                                // than looking dead until the sheet appears.
                                busy = sharing,
                                onClick = {
                                    if (!sharing) {
                                        sharing = true
                                        Analytics.log(context, Analytics.ROUND_RESULT_SHARED)
                                        scope.launch {
                                            writeRoundShareImage(context, info)
                                                ?.let { shareRoundImage(context, it) }
                                            sharing = false
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                state.replayPath?.let { path ->
                    ReplayCard(
                        onPlay = {
                            Analytics.log(context, Analytics.REPLAY_PLAY)
                            playing = path
                        },
                    )
                }

                // Only where there is a next round to film. On the last one the button below
                // says FINISH, and offering a choice about a round that will not happen is
                // just something else to read.
                if (captureMomentAvailable && state.nextTeamName != null) {
                    CCard(Modifier.fillMaxWidth()) {
                        CaptureMomentRow(
                            checked = captureMoment,
                            title = stringResource(R.string.capture_moment_next_round),
                            style = CaptureMomentStyle.OnCard,
                            onCheckedChange = onCaptureMomentChange,
                            onInfo = { showCaptureInfo = true },
                        )
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

/** The compact accent pill inside the "this round" card: label first, share glyph after it. */
@Composable
private fun SharePill(busy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50))
            .background(if (busy) Alias.accentPressed else Alias.accent)
            .clickable(enabled = !busy) { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.share_round_results), color = Alias.onAccent,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
            letterSpacing = 0.4.sp)
        Spacer(Modifier.size(8.dp))
        Icon(Icons.Filled.Share, contentDescription = null, tint = Alias.onAccent,
            modifier = Modifier.size(16.dp))
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
