package hr.azzi.socialgames.alias.ui.screens.online

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    BackHandler { store.leaveAndTeardown() }

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

    val micOn by store.video.micOn.collectAsState()
    val camOn by store.video.cameraOn.collectAsState()

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            TopBar(title = stringResource(R.string.online_lobby_title), onBack = { store.leaveAndTeardown() })
            Spacer(Modifier.height(16.dp))

            // room header card: title + video-room badge, code chip + invite
            CCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(room.deckName.ifEmpty { stringResource(R.string.online_title) }, color = Alias.textPrimary,
                            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.blue600.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("📹 " + stringResource(R.string.online_video_room_badge), color = Alias.blue600, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Alias.fieldBg)
                                .clickable { clipboard.setText(AnnotatedString(room.code)) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(room.code, color = Alias.blue600, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ContentCopy, null, tint = Alias.blue600, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.online_invite), color = Alias.blue600, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.5.sp,
                            modifier = Modifier.clip(RoundedCornerShape(percent = 50)).clickable { shareRoom(context, room.code) }.padding(horizontal = 12.dp, vertical = 10.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline(stringResource(R.string.online_in_the_room))
                Spacer(Modifier.weight(1f))
                Text("${room.players.size} / ${room.maxPlayers}", color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                room.players.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { p ->
                            OnlineVideoTile(store, p, isHost = p.id == room.hostId, modifier = Modifier.weight(1f).height(120.dp))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(4.dp))
                // your camera & mic
                CCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.online_cam_mic_title), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.online_cam_mic_sub), color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 12.sp)
                        }
                        CircleToggle(if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff, micOn) { store.toggleMic() }
                        Spacer(Modifier.width(10.dp))
                        CircleToggle(if (camOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff, camOn) { store.toggleCamera() }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (store.amHost()) {
                PillButton(stringResource(R.string.online_start_match), enabled = canStart) { store.startGame() }
            } else {
                Text(stringResource(R.string.online_waiting_start), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            OutlineDashedButton(stringResource(R.string.online_leave_room)) { store.leaveAndTeardown() }
        }
    }
}

@Composable
private fun CircleToggle(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(if (active) Color.White else Alias.danger.copy(alpha = 0.14f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (active) Alias.blue600 else Alias.danger, modifier = Modifier.size(22.dp))
    }
}

private fun shareRoom(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.online_share_room, code))
    }
    context.startActivity(Intent.createChooser(send, null))
}
