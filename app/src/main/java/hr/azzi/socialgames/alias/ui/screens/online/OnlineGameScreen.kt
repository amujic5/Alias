package hr.azzi.socialgames.alias.ui.screens.online

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
    var showVideoRoom by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }
    val hostControls = store.amHost() && room.status != OnlineStatus.FINISHED

    // Host back = confirm finishing the game; non-host back is swallowed (no leaving mid-game).
    BackHandler { if (hostControls) showStopConfirm = true }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text(stringResource(R.string.online_finish_game_q)) },
            confirmButton = { TextButton(onClick = { showStopConfirm = false; store.stopGame() }) { Text(stringResource(R.string.online_finish_game)) } },
            dismissButton = { TextButton(onClick = { showStopConfirm = false }) { Text(stringResource(R.string.online_keep_playing)) } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        BrandBackground {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                GameTopCard(store, room, onVideoRoom = { showVideoRoom = true }, onStop = if (hostControls) ({ showStopConfirm = true }) else null)
                Spacer(Modifier.height(8.dp))
                TimerBar(store, room)
                Spacer(Modifier.height(12.dp))
                VideoStage(store, room, Modifier.weight(1f))
                Spacer(Modifier.height(14.dp))
                ActiveControls(store, room)
            }
        }

        // phase overlays (modals / sheets over the game chrome)
        when {
            room.status == OnlineStatus.FINISHED -> Scrim(bottom = true) { FinalSheet(store, room) }
            room.roundPhase == OnlineRoundPhase.READY -> Scrim { ReadyModal(store, room) }
            room.roundPhase == OnlineRoundPhase.REVIEW -> Scrim(bottom = true) { EditAnswersSheet(store, room) }
            room.roundPhase == OnlineRoundPhase.STANDINGS -> Scrim(bottom = true) { SummarySheet(store, room) }
        }
        if (showVideoRoom) VideoRoomOverlay(store, room) { showVideoRoom = false }
    }
}

/* --------------------------------------------------------------- chrome */

@Composable
private fun GameTopCard(store: OnlineStore, room: OnlineRoom, onVideoRoom: () -> Unit, onStop: (() -> Unit)?) {
    val now by store.now.collectAsState()
    val team = room.currentTeam
    val secs = room.secondsRemaining(now).coerceAtLeast(0)
    val timeUp = room.status == OnlineStatus.FINISHED
    CCard(Modifier.fillMaxWidth(), padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(teamColor(team?.colorIndex ?: 0)))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(team?.name ?: stringResource(R.string.online_title), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(stringResource(R.string.online_correct_skipped, room.correctThisRound, room.skippedThisRound), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 12.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.fieldBg).padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(if (timeUp) "0:00" else "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}",
                    color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Alias.blue600).clickable { onVideoRoom() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.GridView, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
            if (onStop != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Alias.danger).clickable { onStop() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Stop, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun TimerBar(store: OnlineStore, room: OnlineRoom) {
    val now by store.now.collectAsState()
    val total = room.roundSeconds.coerceAtLeast(1)
    val frac = if (room.roundPhase == OnlineRoundPhase.ACTIVE) (room.secondsRemaining(now).toFloat() / total).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(percent = 50)).background(Color.White.copy(alpha = 0.22f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(percent = 50)).background(Alias.accent))
    }
}

@Composable
private fun VideoStage(store: OnlineStore, room: OnlineRoom, modifier: Modifier) {
    val role = room.role(store.myUid)
    val explainer = room.players.firstOrNull { it.id == room.currentExplainerId }
    val guesser = room.players.firstOrNull { it.id == room.currentGuesserId }
    val me = room.players.firstOrNull { it.id == store.myUid }

    if (role == OnlineRole.SPECTATOR) {
        // Two equal tiles: explainer → guesser, with an arrow between them.
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StageTile(store, explainer, OnlineRole.EXPLAINER, corner = 18, modifier = Modifier.weight(1f).fillMaxHeight())
            Text("→", color = Color(0xFFCBD5E1), fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 26.sp)
            StageTile(store, guesser, OnlineRole.GUESSER, corner = 18, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        // Big tile of my partner + a small self preview (picture-in-picture).
        val big = if (role == OnlineRole.EXPLAINER) guesser else explainer
        val bigRole = if (role == OnlineRole.EXPLAINER) OnlineRole.GUESSER else OnlineRole.EXPLAINER
        Box(modifier.fillMaxWidth()) {
            StageTile(store, big, bigRole, corner = 22, spotlight = true, modifier = Modifier.fillMaxSize())
            if (me != null && me.id != big?.id) {
                OnlineVideoTile(store, me, corner = 14, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).width(92.dp).height(116.dp))
            }
        }
    }
}

@Composable
private fun StageTile(store: OnlineStore, player: OnlinePlayer?, role: OnlineRole, corner: Int, spotlight: Boolean = false, modifier: Modifier) {
    if (player != null) {
        OnlineVideoTile(store, player, role = role, corner = corner, spotlight = spotlight, modifier = modifier)
    } else {
        Box(modifier.clip(RoundedCornerShape(corner.dp)).background(Brush.verticalGradient(listOf(Alias.brandTop, Color(0xFF16233F)))))
    }
}

/* --------------------------------------------------------------- active */

@Composable
private fun ActiveControls(store: OnlineStore, room: OnlineRoom) {
    val words by store.words.collectAsState()
    val role = room.role(store.myUid)
    val word = words.getOrNull(room.wordCursor)

    // After SKIP/CORRECT, block both buttons for 0.5s to prevent double-taps racing
    // the shared word cursor (matches iOS onMark).
    val roundActive = room.roundPhase == OnlineRoundPhase.ACTIVE
    var cooldown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun mark(correct: Boolean) {
        if (cooldown || !roundActive) return
        store.mark(correct)
        cooldown = true
        scope.launch { delay(500); cooldown = false }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        val visible = roundActive && role != OnlineRole.GUESSER
        CCard(Modifier.fillMaxWidth(), padding = 22) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Overline(
                    when {
                        !visible -> stringResource(R.string.online_word_hidden)
                        store.amExplainer() -> stringResource(R.string.online_your_word)
                        else -> stringResource(R.string.online_theyre_guessing)
                    },
                    color = Alias.textSecondary,
                )
                Spacer(Modifier.height(6.dp))
                if (visible) {
                    Text(word ?: "—", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, textAlign = TextAlign.Center)
                } else {
                    Text("• • • • • •", color = Color(0xFFB8C2D1), fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        MicCamChips(store)
        Spacer(Modifier.height(14.dp))
        when (role) {
            OnlineRole.EXPLAINER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigActionButton(stringResource(R.string.online_skip), Icons.Filled.SkipNext, Alias.danger, Modifier.weight(1f), enabled = !cooldown) { mark(false) }
                BigActionButton(stringResource(R.string.online_correct), Icons.Filled.Check, Alias.success, Modifier.weight(1f), enabled = !cooldown) { mark(true) }
            }
            OnlineRole.GUESSER -> PillButton(stringResource(R.string.online_shout_guess)) { }
            else -> Text(stringResource(R.string.online_theyre_guessing), color = Color.White.copy(alpha = 0.9f), fontFamily = Alias.body, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun MicCamChips(store: OnlineStore) {
    val micOn by store.video.micOn.collectAsState()
    val camOn by store.video.cameraOn.collectAsState()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AvChip(if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff, if (micOn) stringResource(R.string.online_mic_on) else stringResource(R.string.online_mic_off), micOn) { store.toggleMic() }
        AvChip(if (camOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff, if (camOn) stringResource(R.string.online_camera_on) else stringResource(R.string.online_camera_off), camOn) { store.toggleCamera() }
    }
}

@Composable
private fun AvChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val tint = if (on) Alias.success else Color.White
    val bg = if (on) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.16f)
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50)).background(bg).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun BigActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 58.dp).clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = if (enabled) 1f else 0.5f)).clickable(enabled = enabled) { onClick() }.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

/* --------------------------------------------------------------- scrims */

@Composable
private fun Scrim(bottom: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            // Swallow taps so the game footer behind the dim isn't clickable.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding().padding(horizontal = if (bottom) 0.dp else 22.dp),
        contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.Center,
    ) { content() }
}

@Composable
private fun SheetCard(scroll: Boolean = false, heightFraction: Float? = null, content: @Composable ColumnScope.() -> Unit) {
    val screenH = LocalConfiguration.current.screenHeightDp
    val heightMod = if (heightFraction != null) Modifier.height((screenH * heightFraction).dp) else Modifier.heightIn(max = (screenH * 0.92f).dp)
    Column(
        Modifier.fillMaxWidth().then(heightMod).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Alias.surface)
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(20.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(percent = 50)).background(Alias.divider))
        Spacer(Modifier.height(14.dp))
        content()
    }
}

/* --------------------------------------------------------------- ready */

@Composable
private fun ReadyModal(store: OnlineStore, room: OnlineRoom) {
    val role = room.role(store.myUid)
    val explainer = room.nameOf(room.currentExplainerId)
    val guesser = room.nameOf(room.currentGuesserId)
    val emoji = when (role) { OnlineRole.EXPLAINER -> "📢"; OnlineRole.GUESSER -> "👂"; else -> "👀" }
    val title = when (role) {
        OnlineRole.EXPLAINER -> stringResource(R.string.online_your_turn)
        OnlineRole.GUESSER -> stringResource(R.string.online_get_ready)
        else -> stringResource(R.string.online_team_is_up, room.currentTeam?.name ?: "—")
    }
    val desc = when (role) {
        OnlineRole.EXPLAINER -> stringResource(R.string.online_ready_desc_explain, guesser)
        OnlineRole.GUESSER -> stringResource(R.string.online_ready_desc_guess, explainer)
        else -> stringResource(R.string.online_ready_desc_spectate, explainer, guesser)
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Alias.surface).padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Alias.accent.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 30.sp) }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(desc, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("📕  " + room.deckName)
            InfoChip("⏱  " + stringResource(R.string.online_sec, room.roundSeconds))
        }
        Spacer(Modifier.height(8.dp))
        InfoChip("💡  " + stringResource(R.string.online_guesses_chip, guesser))
        Spacer(Modifier.height(22.dp))
        if (role == OnlineRole.EXPLAINER) {
            PillButton(stringResource(R.string.online_start_round)) { store.startRound() }
        } else {
            Text(stringResource(R.string.online_waiting_to_start, explainer), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp)
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.fieldBg).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/* ------------------------------------------------------------ summary */

@Composable
private fun SummarySheet(store: OnlineStore, room: OnlineRoom) {
    val amExplainer = store.amExplainer()
    val team = room.currentTeam
    val nextTeam = room.activeTeams.firstOrNull { it.id != room.currentTeamIndex }
    SheetCard(scroll = true) {
        Text(stringResource(R.string.online_round_complete, room.roundIndex + 1), color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(teamColor(team?.colorIndex ?: 0)))
            Spacer(Modifier.width(8.dp))
            Text(team?.name ?: "—", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        val delta = room.roundDelta
        Text((if (delta >= 0) "+$delta" else "$delta"), color = if (delta >= 0) Alias.success else Alias.danger, fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 44.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(R.string.online_points_this_round), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("✓  " + stringResource(R.string.online_n_correct, room.correctThisRound), Alias.success)
            StatChip("⤼  " + stringResource(R.string.online_n_skipped, room.skippedThisRound), Alias.textSecondary)
        }
        Spacer(Modifier.height(18.dp))
        Overline(stringResource(R.string.online_standings_header, room.roundIndex + 1), color = Alias.textSecondary)
        Spacer(Modifier.height(8.dp))
        room.standings(projected = true).forEachIndexed { i, t ->
            StandingRow(i + 1, t, room, if (t.id == room.currentTeamIndex) delta else null, highlight = i == 0)
            Spacer(Modifier.height(8.dp))
        }
        if (nextTeam != null) {
            Spacer(Modifier.height(6.dp))
            Divider(color = Alias.divider)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(teamColor(nextTeam.colorIndex)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.online_up_next, nextTeam.name), color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(nextTeam.name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (amExplainer) {
            PillButton(stringResource(R.string.online_finish_round)) { store.finishRound() }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { store.editAnswers() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.online_edit_answers).uppercase(), color = Alias.blue600, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        } else {
            Text(stringResource(R.string.online_waiting_continue, room.nameOf(room.currentExplainerId)), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(percent = 50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(text, color = color, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun StandingRow(rank: Int, team: OnlineTeam, room: OnlineRoom, delta: Int?, highlight: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (highlight) Alias.accent.copy(alpha = 0.16f) else Alias.fieldBg).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (rank == 1) Alias.accent else Alias.divider), contentAlignment = Alignment.Center) {
            Text("$rank", color = if (rank == 1) Alias.onAccent else Alias.textSecondary, fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(11.dp).clip(CircleShape).background(teamColor(team.colorIndex)))
        Spacer(Modifier.width(8.dp))
        Text(team.name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        if (room.isEliminated(team)) { Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.online_out), color = Alias.danger, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp) }
        Spacer(Modifier.weight(1f))
        if (delta != null) {
            StatChip((if (delta >= 0) "+$delta" else "$delta"), Alias.success)
            Spacer(Modifier.width(8.dp))
        }
        Text("${room.standingScore(team, projected = true)}", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

/* --------------------------------------------------------- edit answers */

@Composable
private fun EditAnswersSheet(store: OnlineStore, room: OnlineRoom) {
    val edited by store.editedResults.collectAsState()
    val results = if (store.amExplainer()) (edited ?: room.roundResults) else room.roundResults
    val amExplainer = store.amExplainer()
    val explainer = room.nameOf(room.currentExplainerId)
    val correct = results.count { it.correct }
    val skipped = results.count { !it.correct }

    SheetCard(heightFraction = 0.7f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (amExplainer) stringResource(R.string.online_edit_answers) else stringResource(R.string.online_answers),
                    color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.height(2.dp))
                Text(if (amExplainer) stringResource(R.string.online_fix_mistaps) else stringResource(R.string.online_reviewing, explainer),
                    color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
            }
            StatChip(stringResource(R.string.online_answers_count, correct, skipped), Alias.success)
        }
        Spacer(Modifier.height(12.dp))
        if (results.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.online_no_words), color = Alias.textSecondary, fontFamily = Alias.body) }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Alias.fieldBg).padding(horizontal = 16.dp)) {
                itemsIndexed(results) { i, mw ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(mw.word, color = if (mw.correct) Alias.textPrimary else Alias.danger, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        if (amExplainer) {
                            Switch(
                                checked = mw.correct, onCheckedChange = { store.toggleAnswer(mw.word) },
                                colors = SwitchDefaults.colors(checkedTrackColor = Alias.success, uncheckedTrackColor = Alias.divider, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White),
                            )
                        } else {
                            Icon(
                                if (mw.correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null,
                                tint = if (mw.correct) Alias.success else Alias.danger, modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    if (i < results.lastIndex) Divider(color = Alias.divider)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (amExplainer) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(stringResource(R.string.online_cancel), kind = PillKind.Dark, fillWidth = false, modifier = Modifier.weight(1f)) { store.discardAndAdvance() }
                PillButton(stringResource(R.string.online_save), fillWidth = false, modifier = Modifier.weight(1f)) { store.saveAnswers() }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Alias.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.online_waiting_save, explainer), color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/* ------------------------------------------------------------- final */

@Composable
private fun FinalSheet(store: OnlineStore, room: OnlineRoom) {
    val winner = room.winner
    val ranked = room.standings(projected = false)
    SheetCard(scroll = true) {
        Text(stringResource(R.string.online_final_results), color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))
        Text("🏆", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.online_team_wins, winner?.name ?: "—"), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
        if (ranked.size >= 2) {
            Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Text("${room.standingScore(ranked[0], projected = false)}", color = teamColor(ranked[0].colorIndex), fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 40.sp)
                Text("  –  ", color = Alias.textSecondary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                Text("${room.standingScore(ranked[1], projected = false)}", color = teamColor(ranked[1].colorIndex), fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 40.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Overline(stringResource(R.string.online_final_results), color = Alias.textSecondary)
        Spacer(Modifier.height(8.dp))
        ranked.forEachIndexed { i, t ->
            FinalStandingRow(i, t, room, isWinner = t.id == winner?.id)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(14.dp))
        if (store.amHost()) {
            PillButton(stringResource(R.string.online_home), kind = PillKind.Dark) { store.stopGame() }
        } else {
            Text(stringResource(R.string.online_waiting_host), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun FinalStandingRow(index: Int, team: OnlineTeam, room: OnlineRoom, isWinner: Boolean) {
    val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "" }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isWinner) Alias.accent.copy(alpha = 0.16f) else Alias.fieldBg).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(medal.ifEmpty { "${index + 1}" }, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(11.dp).clip(CircleShape).background(teamColor(team.colorIndex)))
        Spacer(Modifier.width(8.dp))
        Text(team.name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        if (isWinner) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.accent).padding(horizontal = 10.dp, vertical = 3.dp)) {
                Text(stringResource(R.string.online_winner_badge), color = Alias.onAccent, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("${room.standingScore(team, projected = false)}", color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

/* ------------------------------------------------------- video room */

@Composable
private fun VideoRoomOverlay(store: OnlineStore, room: OnlineRoom, onClose: () -> Unit) {
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color(0xFF0A101E))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.online_video_room), color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                room.players.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { p -> OnlineVideoTile(store, p, role = room.role(p.id), isHost = p.id == room.hostId, modifier = Modifier.weight(1f).height(150.dp)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            MicCamChips(store)
            Spacer(Modifier.height(12.dp))
            PillButton(stringResource(R.string.online_back_to_game)) { onClose() }
        }
    }
}
