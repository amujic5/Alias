package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * "Alias Arena · Live multiplayer rounds" — the one bright thing on the hub.
 *
 * Android port of iOS `ArenaEntryCard.swift`.
 *
 * It used to be a mode card wearing a different colour, built to the same tile and corner
 * as Practice and Challenge so the column would not drift. That made it a peer of the two
 * rows under it, which is not what it is: those are things you can do, this is the thing
 * the app wants you to do. So it is bigger, it glows, and it carries its own LIVE badge
 * rather than hanging one off its corner.
 */
@Composable
fun ArenaEntryCard(
    isLive: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            // The glow is the whole reason this reads as the headline rather than as a
            // yellow row: it is the only thing on the screen that lights what is around it.
            .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = Alias.accent, spotColor = Alias.accent)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFD95A), Color(0xFFFFC529)))
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA92B), Color(0xFFF28C16)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("AR", fontFamily = Alias.display, fontSize = 24.sp, color = Color.White)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.arena_entry_title),
                    fontFamily = Alias.display, fontSize = 27.sp, color = Alias.onAccent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.arena_entry_subtitle),
                    fontFamily = Alias.body, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    color = Alias.onAccent.copy(alpha = 0.72f), maxLines = 2,
                )
            }

            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronRight, null, tint = Alias.onAccent)
            }
        }

        if (isLive) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ArenaPalette.live)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Podcasts, null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text(
                    stringResource(R.string.arena_live),
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 12.sp,
                    color = Color.White, maxLines = 1,
                )
            }
        }
    }
}
