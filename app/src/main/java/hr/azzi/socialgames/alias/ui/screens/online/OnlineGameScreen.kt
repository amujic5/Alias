package hr.azzi.socialgames.alias.ui.screens.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.BuildConfig
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.online.*
import hr.azzi.socialgames.alias.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreen(store: OnlineStore, room: OnlineRoom) {
    BrandBackground {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { store.leaveAndTeardown() })
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.online_standings_header, room.roundIndex + 1), color = Color.White.copy(alpha = 0.8f), fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(10.dp))
            if (room.status == OnlineStatus.PLAYING) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(room.players) { p ->
                        OnlineVideoTile(store, p, role = room.role(p.id), modifier = Modifier.size(width = 100.dp, height = 74.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when {
                    room.status == OnlineStatus.FINISHED -> FinalResults(store, room)
                    room.roundPhase == OnlineRoundPhase.READY -> ReadyView(store, room)
                    room.roundPhase == OnlineRoundPhase.ACTIVE -> ActiveView(store, room)
                    room.roundPhase == OnlineRoundPhase.REVIEW -> ReviewView(store, room)
                    room.roundPhase == OnlineRoundPhase.STANDINGS -> StandingsView(store, room)
                }
            }
        }
    }
}

@Composable
private fun ReadyView(store: OnlineStore, room: OnlineRoom) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DisplayText(stringResource(R.string.online_team_is_up, room.currentTeam?.name ?: "—"), size = 26)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.online_about_to_explain, room.nameOf(room.currentExplainerId)), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))
        if (store.amExplainer()) {
            PillButton(stringResource(R.string.online_start_round), fillWidth = false) { store.startRound() }
        } else {
            Text(stringResource(R.string.online_waiting_for, room.nameOf(room.currentExplainerId)), color = Color.White, fontFamily = Alias.body, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ActiveView(store: OnlineStore, room: OnlineRoom) {
    val now by store.now.collectAsState()
    val words by store.words.collectAsState()
    val seconds = room.secondsRemaining(now)
    val role = room.role(store.myUid)
    val word = words.getOrNull(room.wordCursor)

    var cooldown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun mark(correct: Boolean) {
        if (cooldown) return
        store.mark(correct)
        if (!BuildConfig.DEBUG) { cooldown = true; scope.launch { delay(500); cooldown = false } }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("$seconds", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.online_correct_skipped, room.correctThisRound, room.skippedThisRound), color = Color.White.copy(alpha = 0.8f), fontFamily = Alias.body, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        CCard(Modifier.fillMaxWidth(), padding = 28) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (role == OnlineRole.GUESSER) {
                    Text(stringResource(R.string.online_word_hidden), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 16.sp)
                } else {
                    Text(word ?: "—", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        when (role) {
            OnlineRole.EXPLAINER -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(stringResource(R.string.online_skip), kind = PillKind.Light, enabled = !cooldown, fillWidth = false, modifier = Modifier.weight(1f)) { mark(false) }
                PillButton(stringResource(R.string.online_correct), enabled = !cooldown, fillWidth = false, modifier = Modifier.weight(1f)) { mark(true) }
            }
            OnlineRole.GUESSER -> Text(stringResource(R.string.online_shout_guess), color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else -> Text(stringResource(R.string.online_theyre_guessing), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ReviewView(store: OnlineStore, room: OnlineRoom) {
    val edited by store.editedResults.collectAsState()
    val results = edited ?: room.roundResults
    val amExplainer = store.amExplainer()

    Column(Modifier.fillMaxSize()) {
        DisplayText(if (amExplainer) stringResource(R.string.online_edit_answers) else stringResource(R.string.online_reviewing, room.nameOf(room.currentExplainerId)), size = 22)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.online_answers_count, room.correctThisRound, room.skippedThisRound), color = Color.White.copy(alpha = 0.8f), fontFamily = Alias.body, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        CCard(Modifier.fillMaxWidth().weight(1f)) {
            if (results.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.online_no_words), color = Alias.textSecondary, fontFamily = Alias.body) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results) { mw ->
                        Row(
                            Modifier.fillMaxWidth().then(if (amExplainer) Modifier.clickable { store.toggleAnswer(mw.word) } else Modifier).padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (mw.correct) "✓" else "✗", color = if (mw.correct) Alias.success else Alias.danger, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text(mw.word, color = Alias.textPrimary, fontFamily = Alias.body, fontSize = 16.sp)
                        }
                        Divider(color = Alias.divider)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (amExplainer) {
            PillButton(stringResource(R.string.online_save)) { store.saveAnswers() }
        } else {
            Text(stringResource(R.string.online_reviewing, room.nameOf(room.currentExplainerId)), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun StandingsView(store: OnlineStore, room: OnlineRoom) {
    val amExplainer = store.amExplainer()
    Column(Modifier.fillMaxSize()) {
        DisplayText(stringResource(R.string.online_standings_header, room.roundIndex + 1), size = 20)
        Spacer(Modifier.height(10.dp))
        CCard(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(room.standings(projected = false)) { team ->
                    val out = room.isEliminated(team)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).then(if (out) Modifier.alpha(0.5f) else Modifier), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(teamColor(team.colorIndex)))
                        Spacer(Modifier.width(10.dp))
                        Text(team.name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (out) { Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.online_out), color = Alias.danger, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                        Spacer(Modifier.weight(1f))
                        Text("${team.displayScore}", color = Alias.blue600, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Divider(color = Alias.divider)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (amExplainer) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(stringResource(R.string.online_edit_answers), kind = PillKind.Light, fillWidth = false, modifier = Modifier.weight(1f)) { store.editAnswers() }
                PillButton(stringResource(R.string.online_finish_round), fillWidth = false, modifier = Modifier.weight(1f)) { store.finishRound() }
            }
        } else {
            Text(stringResource(R.string.online_waiting_continue, room.nameOf(room.currentExplainerId)), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun FinalResults(store: OnlineStore, room: OnlineRoom) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏆", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Overline(stringResource(R.string.online_final_results))
        Spacer(Modifier.height(6.dp))
        DisplayText(stringResource(R.string.online_team_wins, room.winner?.name ?: "—"), size = 28)
        Spacer(Modifier.height(24.dp))
        if (store.amHost()) {
            PillButton(stringResource(R.string.online_finish), fillWidth = false) { store.stopGame() }
        } else {
            Text(stringResource(R.string.online_waiting_host), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp)
        }
    }
}
