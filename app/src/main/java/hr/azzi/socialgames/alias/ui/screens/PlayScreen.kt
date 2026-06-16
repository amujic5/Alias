package hr.azzi.socialgames.alias.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.Service.SoundSystem
import hr.azzi.socialgames.alias.ui.theme.Alias
import kotlinx.coroutines.delay

private val FadedNumber = Color(0xFFDCE4EF)

@Composable
fun PlayScreen(
    game: Game,
    soundSystem: SoundSystem,
    onFinished: () -> Unit,
    onQuit: () -> Unit,
) {
    var timeLeft by remember { mutableIntStateOf(game.time) }
    var word by remember { mutableStateOf(game.newWord) }
    var correct by remember { mutableIntStateOf(0) }
    var skip by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(true) }
    var showQuit by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
            if (timeLeft in 1..9) soundSystem.playTikTok()
        }
        // timeLeft == 0 — DON'T flip `running` here: it keys this LaunchedEffect,
        // so changing it would cancel this coroutine before onFinished() runs.
        soundSystem.playEnd()
        delay(800)
        onFinished()
    }

    fun pauseAndShow() { running = false; showSheet = true }

    BackHandler { pauseAndShow() }

    val frac by animateFloatAsState(
        targetValue = if (game.time > 0) timeLeft.toFloat() / game.time else 0f,
        label = "progress",
    )

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top card: team + counts + pause
            Row(
                Modifier.fillMaxWidth().padding(16.dp).shadow(12.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(game.currentTeam.teamName, color = Alias.textPrimary,
                        fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("$correct ${stringResource(R.string.corrected)} · $skip ${stringResource(R.string.skipped)}",
                        color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                }
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Alias.danger)
                        .clickable { pauseAndShow() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Pause, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            }

            // Progress bar
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp)
                .clip(RoundedCornerShape(50)).background(Alias.divider)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(50)).background(Alias.accent))
            }

            // Center: faded countdown + word
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("$timeLeft", color = FadedNumber, fontFamily = Alias.display,
                    fontWeight = FontWeight.ExtraBold, fontSize = 200.sp)
                Text(word, color = Alias.textPrimary, fontFamily = Alias.display,
                    fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp))
            }

            // Bottom action buttons
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ActionButton(stringResource(R.string.skip), Icons.AutoMirrored.Filled.ArrowForward, Alias.danger,
                    Modifier.weight(1f)) {
                    if (!running) return@ActionButton
                    game.addMarkedWord(MarkedWord(word, false)); skip++
                    soundSystem.playSkipButton(); word = game.newWord
                }
                ActionButton(stringResource(R.string.correct), Icons.Filled.Check, Alias.success,
                    Modifier.weight(1f)) {
                    if (!running) return@ActionButton
                    game.addMarkedWord(MarkedWord(word, true)); correct++
                    soundSystem.playRightButton(); word = game.newWord
                }
            }
        }

        if (showSheet) {
            PauseSheet(
                teamName = game.currentTeam.teamName,
                explaining = game.explainingPlayerName,
                answering = game.answeringPlayerName,
                correct = correct,
                skip = skip,
                onResume = { showSheet = false; running = true },
                onStop = { showSheet = false; showQuit = true },
            )
        }
    }

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { showQuit = false; showSheet = true },
            title = { Text(stringResource(R.string.warning), fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(R.string.are_you_sure_quit), fontFamily = Alias.body) },
            confirmButton = {
                TextButton(onClick = onQuit) { Text(stringResource(R.string.yes), color = Alias.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showQuit = false; showSheet = true }) {
                    Text(stringResource(R.string.no), color = Alias.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                         color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(72.dp).shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp)).background(color).clickable { onClick() }.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(8.dp))
        Text(label.uppercase(), color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
private fun PauseSheet(
    teamName: String, explaining: String, answering: String,
    correct: Int, skip: Int, onResume: () -> Unit, onStop: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(enabled = false) {}) {
        Column(
            Modifier.fillMaxWidth().systemBarsPadding().padding(16.dp)
                .shadow(20.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.playing).uppercase(), color = Alias.textSecondary,
                        fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(teamName, color = Alias.textPrimary, fontFamily = Alias.display,
                        fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("$correct ${stringResource(R.string.corrected)} · $skip ${stringResource(R.string.skipped)}",
                        color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                }
                TextButton(onClick = onStop) {
                    Text(stringResource(R.string.stop), color = Alias.danger, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.size(6.dp))
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Alias.success).clickable { onResume() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Alias.divider))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                PlayerColumn(stringResource(R.string.explaining), explaining, Modifier.weight(1f))
                PlayerColumn(stringResource(R.string.answering), answering, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlayerColumn(label: String, name: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = Alias.textSecondary, fontFamily = Alias.body,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
