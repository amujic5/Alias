package hr.azzi.socialgames.alias.ui.screens.online

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideoRenderer
import hr.azzi.socialgames.alias.online.OnlinePlayer
import hr.azzi.socialgames.alias.online.OnlineRole
import hr.azzi.socialgames.alias.online.OnlineStore
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * A single participant tile. Shows the player's initial + name + a live mic
 * indicator (self from the local mic state, others from Stream's remote state).
 * TODO: overlay the Stream VideoRenderer for the live camera feed.
 */
@Composable
fun OnlineVideoTile(store: OnlineStore, player: OnlinePlayer, role: OnlineRole?, modifier: Modifier = Modifier) {
    val micOn by store.video.micOn.collectAsState()
    val muted0 by store.video.mutedUserIds.collectAsState()
    val speaking0 by store.video.speakingUserIds.collectAsState()

    val isMe = player.id == store.myUid
    val muted = if (isMe) !micOn else muted0.contains(player.id)
    val speaking = !muted && speaking0.contains(player.id)

    val participants by store.video.participants.collectAsState()
    val call = store.video.call
    val participant = participants[player.id]

    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(teamColor(player.teamId), Color(0xFF13203A)))),
    ) {
        if (call != null && participant != null) {
            // Live Stream feed; falls back to the participant's initials when the camera is off.
            ParticipantVideoRenderer(call = call, participant = participant)
        } else {
            Box(
                Modifier.align(Alignment.Center).size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(player.initial, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }
        // role badge (top-left)
        if (role != null && role != OnlineRole.SPECTATOR) {
            val label = when (role) {
                OnlineRole.EXPLAINER -> "🎤"
                OnlineRole.GUESSER -> "👂"
                else -> ""
            }
            Text(label, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        // name + mic (bottom)
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null,
                tint = if (muted) Color(0xFFFF6B6B) else if (speaking) Color(0xFF4ADE80) else Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(player.name, color = Color.White, fontFamily = Alias.body, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
