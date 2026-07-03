package hr.azzi.socialgames.alias.ui.screens.online

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import hr.azzi.socialgames.alias.BuildConfig
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.online.*
import hr.azzi.socialgames.alias.ui.theme.*

@Composable
fun OnlineLobbyScreen(store: OnlineStore, room: OnlineRoom) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val canStart = store.amHost() && if (BuildConfig.DEBUG) room.players.size >= 2 else room.players.size == room.maxPlayers

    // --- camera/mic permission ---
    var permAlert by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        permAlert = !res.values.all { it }
    }
    LaunchedEffect(Unit) { if (!hasAvPermissions(context)) launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && permAlert && hasAvPermissions(context)) {
                permAlert = false; store.video.setMic(true); store.video.setCamera(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    if (permAlert) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.online_perm_title)) },
            text = { Text(stringResource(R.string.online_perm_msg)) },
            confirmButton = { TextButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.online_perm_settings)) } },
            dismissButton = { TextButton(onClick = { store.leaveAndTeardown() }) { Text(stringResource(R.string.online_perm_leave)) } },
        )
    }

    BrandBackground {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            TopBar(title = stringResource(R.string.online_lobby_title), onBack = { store.leaveAndTeardown() })
            Spacer(Modifier.height(10.dp))
            // code + copy
            CCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(room.code, color = Alias.blue600, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("⧉", color = Alias.blue600, fontSize = 18.sp, modifier = Modifier.clickable { clipboard.setText(AnnotatedString(room.code)) })
                    Spacer(Modifier.weight(1f))
                    Text("${room.players.size} / ${room.maxPlayers}", color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(14.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // video tiles
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(((room.players.size + 1) / 2 * 128).dp),
                    userScrollEnabled = false,
                ) {
                    items(room.players) { p ->
                        OnlineVideoTile(store, p, role = null, modifier = Modifier.height(118.dp).fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(8.dp))
                // self A/V toggles
                val micOn by store.video.micOn.collectAsState()
                val camOn by store.video.cameraOn.collectAsState()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AvToggle(if (micOn) stringResource(R.string.online_mic_on) else stringResource(R.string.online_mic_off), micOn) { store.toggleMic() }
                    AvToggle(if (camOn) stringResource(R.string.online_camera_on) else stringResource(R.string.online_camera_off), camOn) { store.toggleCamera() }
                }
                Spacer(Modifier.height(14.dp))
                // teams
                CCard(Modifier.fillMaxWidth()) {
                    Column {
                        Overline(stringResource(R.string.online_players_caps), color = Alias.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        room.teams.forEach { team ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(teamColor(team.colorIndex)))
                                Spacer(Modifier.width(8.dp))
                                Text(team.name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.weight(1f))
                                val members = room.players.filter { it.teamId == team.id }.joinToString(" · ") { it.name }
                                Text(members.ifEmpty { "—" }, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (store.amHost()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(stringResource(R.string.online_shuffle_teams), kind = PillKind.Light, fillWidth = false, modifier = Modifier.weight(1f)) { store.shuffleTeams() }
                    PillButton(stringResource(R.string.online_start_match), enabled = canStart, fillWidth = false, modifier = Modifier.weight(1f)) { store.startGame() }
                }
            } else {
                Text(stringResource(R.string.online_waiting_start), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun RowScope.AvToggle(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(if (on) Alias.success.copy(alpha = 0.9f) else Alias.danger.copy(alpha = 0.9f))
            .clickable { onClick() }.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
