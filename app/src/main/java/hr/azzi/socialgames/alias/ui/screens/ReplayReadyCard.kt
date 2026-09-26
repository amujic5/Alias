package hr.azzi.socialgames.alias.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.SocialRegion
import hr.azzi.socialgames.alias.arena.isDarkTheme
import hr.azzi.socialgames.alias.ui.theme.Alias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The pale fills are the light card's. At night the ink on them turns white, so they
// become a faint white wash on the navy card instead of staying pale under white text.
private val CardBorder: Color
    @Composable get() = if (isDarkTheme()) Color.White.copy(alpha = 0.14f) else Color(0xFFDCEBFB)
private val FollowBg: Color
    @Composable get() = if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5FB)
private val ShareBg: Color
    @Composable get() = if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color(0xFFF7FAFF)
private val ShareBorder: Color
    @Composable get() = if (isDarkTheme()) Color.White.copy(alpha = 0.12f) else Color(0xFFE9EEF6)
private val ShareBubble: Color
    @Composable get() = if (isDarkTheme()) Color.White.copy(alpha = 0.14f) else Color.White
private val AliasTop = Color(0xFFE9F1FF)
private val AliasBottom = Color(0xFFEEEAFF)
private val AliasBorder = Color(0xFFDBE7FF)
private val AliasInk = Color(0xFF1F74E0)
private val AliasNote = Color(0xFF6D8FC4)
private val Chevron = Color(0xFF9AA5B6)

/** Where "share with us" is in its lifecycle — the only feedback for a transfer done out of sight. */
enum class ReplaySendState { Idle, Sending, Sent, Failed }

/**
 * The richer replay card — Figma "09.d Results - Ready (replay card)".
 *
 * Shown only where the round's deck language has an account behind it; everywhere else the
 * plain [ReplayCard] stays, because two of the three actions here exist only because there
 * is somewhere to send the video.
 */
@Composable
fun ReplayReadyCard(
    videoPath: String,
    teamName: String,
    region: SocialRegion,
    sendState: ReplaySendState,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onSendToAlias: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(225.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Alias.surface)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.width(128.dp).fillMaxHeight()) {
            Thumbnail(videoPath, teamName, Modifier.height(160.dp).fillMaxWidth(), onPlay)
            Spacer(Modifier.height(6.dp))
            FollowRow(region, Modifier.fillMaxWidth().weight(1f), onFollow)
        }

        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(
                stringResource(R.string.replay_ready_title),
                color = Alias.textPrimary, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1,
            )
            Text(
                stringResource(R.string.replay_ready_subtitle),
                color = Alias.textSecondary, fontFamily = Alias.body,
                fontSize = 9.5.sp, lineHeight = 13.sp, maxLines = 2,
            )
            Spacer(Modifier.height(7.dp))

            ActionRow(
                height = 40.dp, background = null, solid = Alias.accent, border = null,
                icon = Icons.Filled.PlayArrow, iconTint = Color.White, bubble = Alias.onAccent,
                title = stringResource(R.string.replay_play_video), titleColor = Alias.onAccent,
                note = null, noteColor = Alias.textSecondary, chevron = Color(0xFF8A7226),
                onClick = onPlay,
            )
            Spacer(Modifier.height(6.dp))
            ActionRow(
                height = 46.dp, background = null, solid = ShareBg, border = ShareBorder,
                icon = Icons.Filled.IosShare, iconTint = Alias.textPrimary, bubble = ShareBubble,
                title = stringResource(R.string.replay_share_video), titleColor = Alias.textPrimary,
                note = stringResource(R.string.replay_share_video_note), noteColor = Alias.textSecondary,
                chevron = Chevron, onClick = onShare,
            )
            Spacer(Modifier.height(6.dp))
            AliasActionRow(sendState, onSendToAlias)
        }
    }
}

@Composable
private fun AliasActionRow(state: ReplaySendState, onClick: () -> Unit) {
    val title = when (state) {
        ReplaySendState.Sending -> stringResource(R.string.replay_share_alias_sending)
        ReplaySendState.Sent -> stringResource(R.string.replay_share_alias_sent)
        else -> stringResource(R.string.replay_share_alias)
    }
    val note = when (state) {
        ReplaySendState.Sending -> null
        ReplaySendState.Sent -> stringResource(R.string.replay_share_alias_sent_note)
        ReplaySendState.Failed -> stringResource(R.string.replay_share_alias_failed)
        ReplaySendState.Idle -> stringResource(R.string.replay_share_alias_note)
    }
    ActionRow(
        height = 56.dp,
        background = Brush.verticalGradient(listOf(AliasTop, AliasBottom)),
        solid = null, border = AliasBorder,
        // No disc behind this one: the cloud is the mark people recognise from the dialog
        // it opens, and a circle around it reads as a third button style.
        icon = if (state == ReplaySendState.Sent) Icons.Filled.CheckCircle else Icons.Filled.CloudUpload,
        iconTint = if (state == ReplaySendState.Sent) Alias.success else AliasInk,
        bubble = null,
        title = title, titleColor = AliasInk,
        note = note, noteColor = AliasNote, chevron = Color(0xFF7FA4D8),
        spinner = state == ReplaySendState.Sending,
        enabled = state != ReplaySendState.Sending && state != ReplaySendState.Sent,
        onClick = onClick,
    )
}

@Composable
private fun ActionRow(
    height: Dp,
    background: Brush?,
    solid: Color?,
    border: Color?,
    icon: ImageVector,
    iconTint: Color,
    bubble: Color?,
    title: String,
    titleColor: Color,
    note: String?,
    noteColor: Color,
    chevron: Color,
    spinner: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    var box = Modifier
        .fillMaxWidth()
        .height(height)
        .clip(shape)
    if (background != null) box = box.background(background)
    if (solid != null) box = box.background(solid)
    if (border != null) box = box.border(1.dp, border, shape)
    if (enabled) box = box.clickable { onClick() }

    Row(
        box.padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(24.dp).let { if (bubble != null) it.clip(CircleShape).background(bubble) else it },
            contentAlignment = Alignment.Center,
        ) {
            when {
                spinner -> CircularProgressIndicator(Modifier.size(16.dp), color = iconTint, strokeWidth = 2.dp)
                else -> Icon(icon, null, tint = iconTint, modifier = Modifier.size(if (bubble == null) 23.dp else 13.dp))
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                title, color = titleColor, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp, lineHeight = 12.sp,
                maxLines = 1,
            )
            if (note != null) {
                Text(
                    note, color = noteColor, fontFamily = Alias.body,
                    fontSize = 8.sp, lineHeight = 10.sp, maxLines = 2,
                )
            }
        }

        Icon(Icons.Filled.ChevronRight, null, tint = chevron, modifier = Modifier.size(10.dp))
    }
}

/**
 * A frame from the clip, taken a beat in rather than at zero: the first frames are often
 * the camera still settling, and a black rectangle is a worse thumbnail than none.
 */
@Composable
private fun Thumbnail(path: String, teamName: String, modifier: Modifier, onPlay: () -> Unit) {
    var frame by remember(path) { mutableStateOf<Bitmap?>(null) }
    var duration by remember(path) { mutableStateOf("") }

    LaunchedEffect(path) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(path)
                val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val at = minOf(600_000L, maxOf(0L, millis * 500))
                frame = retriever.getFrameAtTime(at, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                val seconds = (millis / 1000).toInt()
                duration = "%d:%02d".format(seconds / 60, seconds % 60)
            }
            runCatching { retriever.release() }
        }
    }

    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(Alias.darkFill).clickable { onPlay() },
    ) {
        frame?.let {
            Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        Pill(teamName.uppercase(), Alias.blue600, Color.White, Modifier.align(Alignment.TopStart).padding(6.dp))
        if (duration.isNotEmpty()) {
            Pill(duration, Color.Black.copy(alpha = 0.55f), Color.White, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }

        Box(
            Modifier.align(Alignment.Center).size(40.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            // Fixed dark: the disc is white in both appearances.
            Icon(Icons.Filled.PlayArrow, null, tint = Alias.darkFill, modifier = Modifier.size(20.dp))
        }

        Box(
            Modifier.align(Alignment.BottomEnd).padding(6.dp).size(22.dp)
                .clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Fullscreen, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun Pill(text: String, background: Color, ink: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(7.dp)).background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text, color = ink, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = 7.5.sp, maxLines = 1,
        )
    }
}

/**
 * The invitation under the still.
 *
 * Two lines rather than a smaller font: every translation of this runs past the width of
 * the column, and "Follow us on / TikTok" reads better than the whole of it shrunk to fit.
 */
@Composable
private fun FollowRow(region: SocialRegion, modifier: Modifier, onFollow: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(FollowBg)
            .clickable { onFollow() }.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_tiktok), null, Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.replay_follow_tiktok),
                color = Alias.textPrimary, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 2,
            )
            // The handle is one word that cannot wrap, and at 8.5sp "@aliaswordsbalkan"
            // needs almost exactly the width this column has — so on a narrower phone, or
            // with a longer handle for the next region, it would be cut off mid-word.
            // Shrinking a point or two is better than ellipsising a name.
            BasicText(
                text = region.tiktokHandle,
                style = TextStyle(
                    color = Alias.textSecondary,
                    fontFamily = Alias.body,
                    fontSize = 8.5.sp,
                    lineHeight = 9.5.sp,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 6.5.sp,
                    maxFontSize = 8.5.sp,
                    stepSize = 0.25.sp,
                ),
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Chevron, modifier = Modifier.size(10.dp))
    }
}
