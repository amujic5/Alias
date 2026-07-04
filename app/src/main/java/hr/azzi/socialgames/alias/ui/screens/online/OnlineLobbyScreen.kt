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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import hr.azzi.socialgames.alias.BuildConfig
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.online.*
import hr.azzi.socialgames.alias.ui.theme.*
import kotlin.math.roundToInt

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

    // --- host drag-and-drop of players between teams (mirrors iOS) ---
    // All positions are kept in root coordinates so the floating "ghost" (drawn
    // in the overlay Box) and the team-card hit-testing share one coordinate space.
    var draggingPlayer by remember { mutableStateOf<OnlinePlayer?>(null) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val teamBounds = remember { mutableStateMapOf<Int, Rect>() }
    val targetedTeam by remember {
        derivedStateOf {
            if (draggingPlayer == null) null
            else teamBounds.entries.firstOrNull { it.value.contains(dragPointer) }?.key
        }
    }

    BrandBackground {
      Box(Modifier.fillMaxSize().onGloballyPositioned { overlayOrigin = it.positionInRoot() }) {
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

                // teams — visible to everyone (read-only for non-hosts); the host can
                // drag a player between teams (long-press to pick up).
                val isHost = store.amHost()
                if (isHost) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(percent = 50)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.OpenWith, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(stringResource(R.string.online_drag_hint), color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                room.teams.forEach { team ->
                    Spacer(Modifier.height(12.dp))
                    TeamCard(
                        store, room, team, isHost,
                        targeted = targetedTeam == team.id,
                        draggingId = draggingPlayer?.id,
                        onBounds = { teamBounds[team.id] = it },
                        onDragStart = { player, ptr -> draggingPlayer = player; dragPointer = ptr },
                        onDrag = { ptr -> dragPointer = ptr },
                        onDragEnd = {
                            val t = targetedTeam
                            val pl = draggingPlayer
                            if (pl != null && t != null && t != pl.teamId) store.movePlayer(pl.id, t)
                            draggingPlayer = null
                        },
                        onDragCancel = { draggingPlayer = null },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            if (store.amHost()) {
                PillButton("🔀  " + stringResource(R.string.online_shuffle_teams), kind = PillKind.Light) { store.shuffleTeams() }
                Spacer(Modifier.height(10.dp))
                PillButton(stringResource(R.string.online_start_match), enabled = canStart) { store.startGame() }
            } else {
                Text(stringResource(R.string.online_waiting_start), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp))
            }
        }

        // Floating "ghost" that follows the finger while a player is being dragged.
        // Offset into overlay-local space (root pointer minus the overlay's origin),
        // nudged up-left so it sits under the fingertip rather than the top-left corner.
        draggingPlayer?.let { ghost ->
            val density = LocalDensity.current
            Box(
                Modifier.offset {
                    IntOffset(
                        (dragPointer.x - overlayOrigin.x - with(density) { 24.dp.toPx() }).roundToInt(),
                        (dragPointer.y - overlayOrigin.y - with(density) { 22.dp.toPx() }).roundToInt(),
                    )
                },
            ) {
                val ghostColor = teamColor(room.teams.firstOrNull { it.id == ghost.teamId }?.colorIndex ?: 0)
                DragGhost(ghost, ghostColor)
            }
        }
      }
    }
}

/** The chip that follows the finger during a drag: avatar + name, lifted with a shadow. */
@Composable
private fun DragGhost(p: OnlinePlayer, color: Color) {
    Row(
        Modifier.shadow(12.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Alias.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(p.initial, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(p.name, color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** One team's card: colored dot, name, count, and its members. Read-only for
 *  everyone; for the host each member row can be long-pressed and dragged onto
 *  another team card (`store.movePlayer` is itself host-guarded). Mirrors iOS. */
@Composable
private fun TeamCard(
    store: OnlineStore,
    room: OnlineRoom,
    team: OnlineTeam,
    isHost: Boolean,
    targeted: Boolean,
    draggingId: String?,
    onBounds: (Rect) -> Unit,
    onDragStart: (OnlinePlayer, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val members = room.players.filter { it.teamId == team.id }.sortedBy { it.joinedAt.time }
    val color = teamColor(team.colorIndex)
    Box(Modifier.fillMaxWidth().onGloballyPositioned { onBounds(it.boundsInRoot()) }) {
      CCard(Modifier.fillMaxWidth(), padding = 14) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(13.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(team.name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                if (targeted) {
                    Box(Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.blue600).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.online_drop_here), color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                } else {
                    Text("${members.size} / 2", color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
            if (members.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Alias.fieldBg).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.online_drop_player), color = Alias.textSecondary.copy(alpha = 0.7f), fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            } else {
                members.forEach { p ->
                    Spacer(Modifier.height(10.dp))
                    TeamMemberRow(
                        store, room, p, color, isHost,
                        dragging = draggingId == p.id,
                        onDragStart = { ptr -> onDragStart(p, ptr) },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                    )
                }
            }
        }
      }
      // Accent outline drawn on top of the card while a player hovers over it.
      if (targeted) Box(Modifier.matchParentSize().border(2.5.dp, Alias.accent, RoundedCornerShape(20.dp)))
    }
}

@Composable
private fun TeamMemberRow(
    store: OnlineStore,
    room: OnlineRoom,
    p: OnlinePlayer,
    color: Color,
    isHost: Boolean,
    dragging: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val you = p.id == store.myUid
    // Row's own top-left in root coordinates, so pointer offsets (which are
    // row-local) can be translated into the shared root space used for hit-testing.
    var rowRoot by remember { mutableStateOf(Offset.Zero) }
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Alias.fieldBg)
        .onGloballyPositioned { rowRoot = it.positionInRoot() }
    val rowMod = if (isHost) {
        base.pointerInput(p.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { off -> onDragStart(rowRoot + off) },
                onDrag = { change, _ -> change.consume(); onDrag(rowRoot + change.position) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragCancel() },
            )
        }
    } else base
    Row(rowMod.alpha(if (dragging) 0.4f else 1f).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isHost) {
            Icon(Icons.Filled.DragIndicator, null, tint = Alias.textSecondary.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(p.initial, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(p.name + if (you) " (${stringResource(R.string.online_you)})" else "", color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (room.isHost(p.id)) {
            Box(Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.accent).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(stringResource(R.string.online_host), color = Alias.onAccent, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
            }
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
