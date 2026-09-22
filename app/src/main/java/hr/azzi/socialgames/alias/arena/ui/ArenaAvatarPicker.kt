package hr.azzi.socialgames.alias.arena.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.AliasAvatar
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.PillButton

/**
 * A sheet of every avatar this build ships, applied the moment one is tapped.
 *
 * Android port of iOS `ArenaAvatarPickerView.swift`.
 *
 * Draws from the catalogue rather than a fixed grid, so it can grow without this screen
 * changing. There is no confirm step: the choice is one field on a profile, it is visible
 * immediately behind the sheet, and asking somebody to commit to a picture adds a decision
 * where none is needed. "Done" closes, it does not save.
 */
@Composable
fun ArenaAvatarPicker(
    selected: AliasAvatar,
    onPick: (AliasAvatar) -> Unit,
    onClose: () -> Unit,
) {
    // Tracked locally so the tick moves under the finger, rather than waiting for a round
    // trip to Firestore to come back and redraw the sheet.
    var choice by remember { mutableStateOf(selected) }

    /**
     * Unlocked first, then the rest in the order they arrive.
     *
     * Catalogue order would scatter what the player can wear among what they cannot, and
     * the sheet exists to choose — not to browse a locked collection. Catalogue position
     * breaks the ties, because a grid that reshuffles under the finger is worse than one
     * in the wrong order.
     */
    val sorted = remember {
        val rank = AliasAvatar.ALL.withIndex().associate { (i, a) -> a.id to i }
        AliasAvatar.ALL.sortedWith(
            compareByDescending<AliasAvatar> { it.isUnlockedByLevel }
                .thenBy { it.unlockLevel }
                .thenBy { rank[it.id] ?: 0 },
        )
    }

    Column(Modifier.fillMaxSize().background(ArenaColors.card)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 26.dp, bottom = 18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.arena_avatar_title),
                    fontFamily = Alias.display, fontSize = 26.sp, color = ArenaColors.title,
                )
                Text(
                    stringResource(R.string.arena_avatar_subtitle),
                    fontFamily = Alias.body, fontSize = 14.sp, color = ArenaColors.body,
                )
            }
            Icon(
                Icons.Filled.Close, null, tint = ArenaColors.body,
                modifier = Modifier.size(24.dp).clickable(onClick = onClose),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(sorted, key = { it.id }) { avatar ->
                AvatarCell(avatar, avatar == choice && avatar.isUnlockedByLevel) {
                    if (avatar.isUnlockedByLevel) {
                        choice = avatar
                        onPick(avatar)
                    }
                }
            }
        }

        PillButton(
            title = stringResource(R.string.arena_avatar_done),
            onClick = onClose,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 20.dp),
        )
    }
}

/**
 * A locked avatar is shown, not hidden.
 *
 * The grid is the only place the catalogue exists, so hiding the nineteen that are still
 * coming would make levelling up produce faces out of nowhere. Shown dimmed with the level
 * they arrive at, they are the reason to keep playing — which is the entire point of
 * putting them behind a level.
 */
@Composable
private fun AvatarCell(avatar: AliasAvatar, on: Boolean, onTap: () -> Unit) {
    val unlocked = avatar.isUnlockedByLevel
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.clickable(enabled = unlocked, onClick = onTap),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier.size(66.dp).clip(CircleShape)
                    .background(if (on) Alias.accent else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                ArenaAvatarImage(
                    avatar.id, 58.dp, ring = null,
                    desaturated = !unlocked,
                    modifier = Modifier.alpha(if (unlocked) 1f else 0.45f),
                )
            }
            if (on || !unlocked) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(Alias.onAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (on) Icons.Filled.Check else Icons.Filled.Lock,
                        null, tint = Color.White, modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Text(
            if (unlocked) stringResource(avatar.nameRes)
            else stringResource(R.string.arena_avatar_locked, avatar.unlockLevel),
            fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            color = if (unlocked) ArenaColors.title else ArenaColors.body,
            textAlign = TextAlign.Center, maxLines = 1,
        )
    }
}
