package hr.azzi.socialgames.alias.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.Analytics
import hr.azzi.socialgames.alias.Service.RoundVideo.AliasSubmission
import hr.azzi.socialgames.alias.Service.RoundVideo.ReplayUploader
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.CCard
import java.io.File

private val TileTop = Color(0xFF3A4654)
private val TileBottom = Color(0xFF1A222B)

/**
 * "Your replay is ready!" — the round video's one entry point (Figma "09.c Results - Ready",
 * node 2415:373).
 *
 * Only shown when a clip actually exists. A round can end without one: the device may have
 * declined the camera, the microphone may have been busy, or the writer may have failed
 * mid-round. None of those are worth a message here — the card simply is not there.
 */
@Composable
fun ReplayCard(onPlay: () -> Unit, modifier: Modifier = Modifier) {
    CCard(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onPlay() }, padding = 14) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(TileTop, TileBottom))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.replay_ready_title),
                    color = Alias.textPrimary, fontFamily = Alias.body,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp,
                )
                Text(
                    stringResource(R.string.replay_ready_subtitle),
                    color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 14.sp,
                )
            }

            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Alias.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Alias.onAccent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Full-screen playback of the round video.
 *
 * `VideoView` rather than a player library: this plays one local file, once, and pulling in
 * a media dependency for that would be the larger change. It brings its own transport
 * controls; the bar below carries what the design asks for besides them.
 *
 * The three actions are labelled rather than left as bare glyphs — "share" and "send to
 * Alias" are one icon apart and impossible to tell from each other without words. The third
 * is only there when [submission] says which audience the clip belongs to.
 */
@Composable
fun ReplayPlayer(
    path: String,
    submission: AliasSubmission? = null,
    /**
     * Whether to say that the upload has started and the game can go on.
     *
     * True only where there is a game to go on with — the results screen between rounds.
     * A clip replayed from the saved list has no round waiting, so the reassurance would be
     * answering a question nobody asked.
     */
    announceSendStart: Boolean = false,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var sendState by remember(path) {
        mutableStateOf(if (ReplayUploader.hasSent(path)) ReplaySendState.Sent else ReplaySendState.Idle)
    }
    var asking by remember { mutableStateOf(false) }

    if (asking) {
        SendToAliasDialog(
            onDismiss = { asking = false },
            onApprove = {
                asking = false
                val target = submission ?: return@SendToAliasDialog
                sendState = ReplaySendState.Sending
                if (announceSendStart) {
                    Toast.makeText(context, R.string.replay_send_started_toast, Toast.LENGTH_LONG).show()
                }
                Analytics.log(context, Analytics.REPLAY_SEND_STARTED)
                ReplayUploader.upload(File(path), target.deckId, target.languageCode, target.folder) { result ->
                    sendState = if (result.isSuccess) ReplaySendState.Sent else ReplaySendState.Failed
                    Analytics.log(
                        context,
                        if (result.isSuccess) Analytics.REPLAY_SEND_SUCCEEDED else Analytics.REPLAY_SEND_FAILED,
                    )
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            setVideoPath(path)
                            setMediaController(MediaController(viewContext).also { it.setAnchorView(this) })
                            setOnPreparedListener { player ->
                                player.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlayerAction(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = stringResource(R.string.player_exit),
                    background = Color.White.copy(alpha = 0.14f), ink = Color.White,
                    modifier = Modifier.weight(1f), onClick = onClose,
                )
                PlayerAction(
                    icon = Icons.Filled.IosShare,
                    title = stringResource(R.string.player_share),
                    background = Color(0xFF1E7BF0), ink = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        Analytics.log(context, Analytics.REPLAY_SHARE)
                        shareReplayFile(context, path)
                    },
                )
                if (submission != null) {
                    PlayerAction(
                        icon = if (sendState == ReplaySendState.Sent) Icons.Filled.CheckCircle
                               else Icons.Filled.CloudUpload,
                        title = if (sendState == ReplaySendState.Sent)
                            stringResource(R.string.replay_share_alias_sent)
                        else stringResource(R.string.player_share_alias),
                        background = Alias.accent, ink = Alias.onAccent,
                        spinner = sendState == ReplaySendState.Sending,
                        enabled = sendState == ReplaySendState.Idle || sendState == ReplaySendState.Failed,
                        modifier = Modifier.weight(1f),
                        onClick = { asking = true },
                    )
                }
            }
        }
    }
}

/** One tile in the player's bottom bar: icon over label, filled, tappable as a whole. */
@Composable
private fun PlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    background: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    spinner: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // 86dp, not 72: maxLines is 2, but at 72 the padding and the icon left 18dp for the
    // label — one line's worth — so the second was simply cut off. Half the translations
    // need it ("Envoie-le-nous", "INVIO RIUSCITO", "Podijeli s nama").
    Column(
        modifier
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .let { if (enabled) it.clickable { onClick() } else it.alpha(0.55f) }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Centred, so a one-line label sits in the middle of the tile rather than riding
        // at its top next to the two-line ones.
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        if (spinner) {
            CircularProgressIndicator(Modifier.size(24.dp), color = ink, strokeWidth = 2.dp)
        } else {
            Icon(icon, null, tint = ink, modifier = Modifier.size(24.dp))
        }
        Text(
            title, color = ink, fontFamily = Alias.body,
            fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
            maxLines = 2, textAlign = TextAlign.Center, lineHeight = 14.sp,
        )
    }
}

fun shareReplayFile(context: Context, path: String) {
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
    }.onFailure {
        // A path the provider has not been told about throws here, and swallowing it is how
        // a share button ends up doing nothing at all.
        Log.w("RoundVideo", "cannot share $path", it)
    }.getOrNull() ?: return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        // The extra grants the app the user picks; the clip data grants the share sheet
        // itself, which is a separate app and is what draws the preview thumbnail. Without
        // it the sheet shows the clip as an unreadable generic attachment.
        clipData = ClipData.newUri(context.contentResolver, "video", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
