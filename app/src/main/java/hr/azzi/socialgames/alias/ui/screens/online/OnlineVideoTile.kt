package hr.azzi.socialgames.alias.ui.screens.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VideocamOff
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideoRenderer
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.online.OnlinePlayer
import hr.azzi.socialgames.alias.online.OnlineRole
import hr.azzi.socialgames.alias.online.OnlineStore
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * A participant tile — live Stream feed when the camera is on, otherwise the
 * player's colored avatar. Shows a status badge (SPEAKING / EXPLAINING /
 * GUESSING / HOST) and a name + mic/camera row along the bottom.
 */
@Composable
fun OnlineVideoTile(
    store: OnlineStore,
    player: OnlinePlayer,
    role: OnlineRole? = null,
    isHost: Boolean = false,
    corner: Int = 18,
    spotlight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val micOn by store.video.micOn.collectAsState()
    val camOn by store.video.cameraOn.collectAsState()
    val muted0 by store.video.mutedUserIds.collectAsState()
    val speaking0 by store.video.speakingUserIds.collectAsState()
    val participants by store.video.participants.collectAsState()
    val call = store.video.call

    val isMe = player.id == store.myUid
    val muted = if (isMe) !micOn else muted0.contains(player.id)
    val cameraOff = isMe && !camOn
    val speaking = !muted && speaking0.contains(player.id)
    val participant = participants[player.id]

    val shape = RoundedCornerShape(corner.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(teamColor(player.teamId).copy(alpha = 0.88f), Color(0xFF16233F))))
            .then(if (speaking) Modifier.border(2.5.dp, Alias.accent, shape) else Modifier),
    ) {
        if (call != null && participant != null && !cameraOff) {
            VideoTheme { ParticipantVideoRenderer(call = call, participant = participant) }
        } else {
            Box(
                Modifier.align(Alignment.Center).size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(player.initial, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }

        // status badge (top-left)
        val badge: Triple<String, Color, Color>? = when {
            speaking -> Triple(stringResource(R.string.online_role_speaking), Color(0xFF0C3B33), Color(0xFF57E08E))
            role == OnlineRole.EXPLAINER -> Triple(stringResource(R.string.online_role_explaining), Alias.blue600, Color.White)
            role == OnlineRole.GUESSER -> Triple(stringResource(R.string.online_role_guessing), Alias.blue600, Color.White)
            isHost -> Triple(stringResource(R.string.online_host), Alias.accent, Alias.onAccent)
            else -> null
        }
        if (badge != null && !spotlight) {
            Box(
                Modifier.align(Alignment.TopStart).padding(8.dp)
                    .clip(RoundedCornerShape(percent = 50)).background(badge.second)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(badge.first, color = badge.third, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.5.sp)
            }
        }

        // name + mic/cam (bottom)
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spotlight && badge != null) {
                Box(
                    Modifier.clip(RoundedCornerShape(percent = 50)).background(badge.second).padding(horizontal = 9.dp, vertical = 3.dp),
                ) { Text(badge.first, color = badge.third, fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.5.sp) }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isMe) stringResource(R.string.online_you) else player.name,
                color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            if (cameraOff) {
                Icon(Icons.Filled.VideocamOff, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null,
                tint = if (muted) Color(0xFFFF6B6B) else if (speaking) Color(0xFF57E08E) else Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
