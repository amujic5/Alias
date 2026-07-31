package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundMediaItem
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import java.text.DateFormat
import java.util.Date

private val ShareFill = Color(0xFFEAF6FF)
private val DeleteFill = Color(0xFFFEECEC)

/**
 * Every round that was filmed (Figma "18 Povijest (History)", node 3215:600).
 *
 * The clips are the only thing in the app that survives a game, so this is also the only
 * place they can be got rid of — each row carries its own delete, and nothing else prunes
 * them.
 */
@Composable
fun RoundHistoryScreen(
    items: List<RoundMediaItem>,
    /** The round with the most correct answers, marked with a star. Null when the list is empty. */
    bestId: String?,
    onBack: () -> Unit,
    onSort: () -> Unit,
    onPlay: (RoundMediaItem) -> Unit,
    onShare: (RoundMediaItem) -> Unit,
    onDelete: (RoundMediaItem) -> Unit,
) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircleButton(onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Alias.textPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    stringResource(R.string.history_title),
                    color = Color.White, fontFamily = Alias.display,
                    fontWeight = FontWeight.ExtraBold, fontSize = 34.sp,
                    letterSpacing = 1.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                CircleButton(onSort) {
                    Icon(Icons.Filled.Sort, null, tint = Alias.textPrimary, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                stringResource(if (items.isEmpty()) R.string.history_empty else R.string.history_hint),
                color = Color.White, fontFamily = Alias.body, fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    RoundCard(
                        item = item,
                        isBest = item.id == bestId,
                        onPlay = { onPlay(item) },
                        onShare = { onShare(item) },
                        onDelete = { onDelete(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundCard(
    item: RoundMediaItem,
    isBest: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { onPlay() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                item.teamName, color = Alias.textPrimary, fontFamily = Alias.display,
                fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
            )
            if (isBest) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Alias.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.history_explains))
            Value(item.explainer)
            Text("|", color = Alias.divider, fontFamily = Alias.body, fontSize = 13.sp)
            Label(stringResource(R.string.history_guesses))
            Value(item.guesser)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBadge(Alias.success, Icons.Filled.Check)
            Text(
                stringResource(R.string.history_correct_count, item.correctCount),
                color = Alias.textPrimary, fontFamily = Alias.body,
                fontWeight = FontWeight.Bold, fontSize = 14.sp,
            )
            Box(Modifier.size(10.dp, 1.dp))
            StatBadge(Alias.accent, Icons.Filled.SkipNext)
            Text(
                stringResource(R.string.history_skipped_count, item.skippedCount),
                color = Alias.textPrimary, fontFamily = Alias.body,
                fontWeight = FontWeight.Bold, fontSize = 14.sp,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Filled.Event, null, tint = Alias.textSecondary, modifier = Modifier.size(16.dp))
            val date = Date(item.recordedAt)
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(date),
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp,
            )
            Text("•", color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
            Text(
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date),
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).height(46.dp).clip(CircleShape).background(Alias.accent)
                    .clickable { onPlay() }.padding(start = 8.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Alias.textPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    stringResource(R.string.history_watch_replay), color = Alias.onAccent,
                    fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                )
            }
            Row(
                Modifier.height(46.dp).clip(CircleShape).background(ShareFill)
                    .clickable { onShare() }.padding(start = 13.dp, end = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Filled.Share, null, tint = Alias.blue600, modifier = Modifier.size(17.dp))
                Text(
                    stringResource(R.string.history_share), color = Alias.blue600,
                    fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                )
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(DeleteFill).clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DeleteOutline, null, tint = Alias.danger, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StatBadge(fill: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
}

@Composable
private fun Value(text: String) {
    Text(
        text, color = Alias.textPrimary, fontFamily = Alias.body,
        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
    )
}

@Composable
private fun CircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Color.White).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}
