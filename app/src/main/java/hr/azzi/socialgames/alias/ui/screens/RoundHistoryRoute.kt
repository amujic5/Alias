package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import hr.azzi.socialgames.alias.Service.Analytics
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundMediaLibrary
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundMediaSort

/**
 * Holds what the history screen needs and does what its buttons ask.
 *
 * Separate from [RoundHistoryScreen] so the screen stays a function of its arguments — it
 * can be looked at without a library behind it, and everything that touches the disk is in
 * one place.
 */
@Composable
fun RoundHistoryRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    var sort by remember { mutableStateOf(RoundMediaSort.NEWEST) }
    // Bumped after a delete so the list is read again; the library is the source of truth
    // and nothing else would tell the screen that a row has gone.
    var revision by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { Analytics.log(context, Analytics.REPLAY_LIST_VISITED) }

    val items = remember(sort, revision) { RoundMediaLibrary.items(context, sort) }
    val best = remember(items) { items.maxByOrNull { it.correctCount }?.id }

    playing?.let { path -> ReplayPlayer(path) { playing = null } }

    RoundHistoryScreen(
        items = items,
        bestId = best,
        onBack = onBack,
        onSort = {
            // One button cycling the orders, as the design has it — a menu for four options
            // that are all one tap apart is more chrome than it is worth.
            sort = RoundMediaSort.entries[(sort.ordinal + 1) % RoundMediaSort.entries.size]
        },
        onPlay = { item ->
            Analytics.log(context, Analytics.REPLAY_LIST_ITEM_PLAY)
            playing = RoundMediaLibrary.fileFor(context, item).absolutePath
        },
        onShare = { item ->
            Analytics.log(context, Analytics.REPLAY_LIST_ITEM_SHARE)
            shareReplayFile(context, RoundMediaLibrary.fileFor(context, item).absolutePath)
        },
        onDelete = { item ->
            Analytics.log(context, Analytics.REPLAY_LIST_ITEM_DELETE)
            RoundMediaLibrary.delete(context, item)
            revision++
        },
    )
}
