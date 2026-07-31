package hr.azzi.socialgames.alias.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import hr.azzi.socialgames.alias.R
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
 * controls, which is the whole of what the design asks for besides sharing.
 */
@Composable
fun ReplayPlayer(path: String, onClose: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
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

            Box(
                Modifier.align(Alignment.TopEnd).padding(16.dp).size(44.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.18f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                    .clip(CircleShape).background(Alias.accent)
                    .clickable { shareReplayFile(context, path) }
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Share, null, tint = Alias.onAccent, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.share), color = Alias.onAccent,
                    fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp, letterSpacing = 0.32.sp,
                )
            }
        }
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
