package hr.azzi.socialgames.alias.ui.screens.ai

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundCardInfo
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundCardWord
import hr.azzi.socialgames.alias.Service.RoundVideo.RoundShareHeader
import hr.azzi.socialgames.alias.Service.RoundVideo.shareRoundImage
import hr.azzi.socialgames.alias.Service.RoundVideo.writeRoundShareImage
import hr.azzi.socialgames.alias.ai.AIAnalytics
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The shareable card for an AI round (Figma node 3340:645): the same result image the local
 * game shares, but headed by the player's mascot instead of a team.
 *
 * Both AI endings use it — the practice summary and a challenge's rank board — so the wiring
 * lives here rather than in either screen.
 */
suspend fun writeAiShareImage(
    context: Context,
    correctWords: List<String>,
    skippedWords: List<String>,
): File? = withContext(Dispatchers.Default) {
    val avatar = AiAvatar.current(context)
    // A missing still is not fatal: the card then draws the disc in the mascot's own tint.
    val mascot = runCatching {
        BitmapFactory.decodeResource(context.resources, avatar.stillRes)
    }.getOrNull()
    // No team, no players: the AI card's header is the mascot, and the renderer reads only
    // the two word lists from here.
    val info = RoundCardInfo(
        teamName = "",
        explainer = "",
        guesser = "",
        // An AI round has no scored deck behind it, so no word carries a value.
        correctWords = correctWords.map { RoundCardWord(it) },
        skippedWords = skippedWords.map { RoundCardWord(it) },
    )
    writeRoundShareImage(
        context = context,
        info = info,
        header = RoundShareHeader.Mascot(mascot, avatar.tint.toArgb()),
        fileName = "ai-round-result.png",
    )
}

/**
 * The share button both AI endings show. Drawing the card takes a moment, so the pill goes
 * quiet rather than looking dead while the sheet is on its way.
 */
@Composable
fun AiSharePill(correctWords: List<String>, skippedWords: List<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    PillButton(
        title = stringResource(R.string.share_round_results),
        kind = PillKind.Light,
        enabled = !sharing,
    ) {
        sharing = true
        AIAnalytics.shareAiResult(context)
        scope.launch {
            writeAiShareImage(context, correctWords, skippedWords)
                ?.let { shareRoundImage(context, it) }
            sharing = false
        }
    }
}
