package hr.azzi.socialgames.alias.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import android.view.Surface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.Service.GameSettings
import hr.azzi.socialgames.alias.Service.SoundSystem
import hr.azzi.socialgames.alias.ui.theme.Alias
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.delay

private val FadedNumber = Color(0xFFDCE4EF)

// Taboo play palette (Figma "14 Play - Taboo").
private val TabooBg = Color(0xFF2A1E3F)
private val TabooCard = Color(0xFF8E63F0)
private val TabooPill = Color(0xFF5C3DA6)

@Composable
fun PlayScreen(
    game: Game,
    soundSystem: SoundSystem,
    onFinished: () -> Unit,
    onQuit: () -> Unit,
    /** Follows the round's own play/pause, so the round video covers the round and nothing else. */
    onRoundRunning: (Boolean) -> Unit = {},
    /** The round video's overlay, fed the same values the screen is showing. */
    onRoundState: (word: String, correct: Int, skipped: Int, remainingSeconds: Int) -> Unit = { _, _, _, _ -> },
    /** True when the round is being filmed, which replaces the word with the live stage. */
    cameraMode: Boolean = false,
    /** Where the compositor should draw the stage. */
    onPreviewSurface: (Surface?, Int, Int) -> Unit = { _, _, _ -> },
    /** False when the round is being filmed with the back camera alone. */
    hasFrontCamera: Boolean = true,
) {
    val context = LocalContext.current
    var timeLeft by remember { mutableIntStateOf(game.time) }
    var word by remember { mutableStateOf(game.newWord) }
    // Alias Junior words are "NNN-word": a picture (res/raw/junior_NNN) the guessers look
    // at, with the clean word shown to the explainer. For every other deck `junior` is null.
    val junior = remember(word) { juniorImageRes(context, word) }
    // Taboo words are "target: forbidden1, forbidden2, …": explain the target without saying
    // any of the forbidden words. `taboo` is null for every non-taboo deck.
    val taboo = remember(word) { parseTaboo(word) }
    val displayWord = junior?.first ?: taboo?.first ?: word
    var correct by remember { mutableIntStateOf(0) }
    var skip by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(true) }
    var showQuit by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        onRoundRunning(running)
        if (!running) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
            if (timeLeft in 1..9) soundSystem.playTikTok()
        }
        // timeLeft == 0 — DON'T flip `running` here: it keys this LaunchedEffect,
        // so changing it would cancel this coroutine before onFinished() runs.
        soundSystem.playEnd()
        delay(800)
        onFinished()
    }

    // The overlay is fed from the same values the screen renders from, so the clip cannot
    // drift from what the players actually saw. `displayWord` rather than `word`: Junior and
    // Taboo decks pack extra data into the raw word, and the card shows the clean one.
    LaunchedEffect(displayWord, correct, skip, timeLeft) {
        onRoundState(displayWord, correct, skip, timeLeft)
    }

    fun pauseAndShow() { running = false; showSheet = true }

    BackHandler { pauseAndShow() }

    val frac by animateFloatAsState(
        targetValue = if (game.time > 0) timeLeft.toFloat() / game.time else 0f,
        label = "progress",
    )

    Box(Modifier.fillMaxSize().background(if (taboo != null) TabooBg else Color.White)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top card: team + counts + pause
            Row(
                Modifier.fillMaxWidth().padding(16.dp).shadow(12.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(game.currentTeam.teamName, color = Alias.textPrimary,
                        fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("$correct ${stringResource(R.string.corrected)} · $skip ${stringResource(R.string.skipped)}",
                        color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                }
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Alias.danger)
                        .clickable { pauseAndShow() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Pause, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            }

            // Progress bar
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (taboo != null) Color.White.copy(alpha = 0.16f) else Alias.divider)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(50)).background(Alias.accent))
            }

            // Center: faded countdown + word (or Junior picture card / Taboo forbidden-words card)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (cameraMode) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp)) {
                        // The word is on the stage's own plate here, not on the screen behind
                        // it — the explainer is looking at the camera, not at the page.
                        CameraStage(
                            word = if (showSheet) "" else displayWord,
                            onSurface = onPreviewSurface,
                            hasFrontCamera = hasFrontCamera,
                        )
                        TimerBadge(
                            timeLeft,
                            Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 8.dp),
                        )
                    }
                } else if (taboo != null) {
                    // Keep the forbidden-words card hidden until the round actually starts —
                    // the pause/pre-start sheet only dims with a translucent scrim, so without
                    // this the card would be readable before the player presses Play.
                    if (!showSheet) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("$timeLeft", color = Color.White, fontFamily = Alias.display,
                                fontWeight = FontWeight.ExtraBold, fontSize = 84.sp)
                            Spacer(Modifier.height(20.dp))
                            TabooCard(target = taboo.first, forbidden = taboo.second)
                        }
                    }
                } else if (junior != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$timeLeft", color = FadedNumber, fontFamily = Alias.display,
                            fontWeight = FontWeight.ExtraBold, fontSize = 64.sp)
                        Spacer(Modifier.height(4.dp))
                        rememberSvgImage(junior.second)?.let { bmp ->
                            Image(bitmap = bmp, contentDescription = displayWord,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(240.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(displayWord, color = Alias.textPrimary, fontFamily = Alias.display,
                            fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp))
                    }
                } else {
                    Text("$timeLeft", color = FadedNumber, fontFamily = Alias.display,
                        fontWeight = FontWeight.ExtraBold, fontSize = 200.sp)
                    Text(word, color = Alias.textPrimary, fontFamily = Alias.display,
                        fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp))
                }
            }

            // Bottom action buttons
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ActionButton(stringResource(R.string.skip), Icons.AutoMirrored.Filled.ArrowForward, Alias.danger,
                    Modifier.weight(1f)) {
                    if (!running) return@ActionButton
                    game.addMarkedWord(MarkedWord(displayWord, false)); skip++
                    soundSystem.playSkipButton(); GameSettings.tick(context); word = game.newWord
                }
                ActionButton(stringResource(R.string.correct), Icons.Filled.Check, Alias.success,
                    Modifier.weight(1f)) {
                    if (!running) return@ActionButton
                    game.addMarkedWord(MarkedWord(displayWord, true)); correct++
                    soundSystem.playRightButton(); GameSettings.tick(context); word = game.newWord
                }
            }
        }

        if (showSheet) {
            PauseSheet(
                teamName = game.currentTeam.teamName,
                explaining = game.explainingPlayerName,
                answering = game.answeringPlayerName,
                correct = correct,
                skip = skip,
                onResume = { showSheet = false; running = true },
                onStop = { showSheet = false; showQuit = true },
            )
        }
    }

    if (showQuit) {
        QuitGameDialog(
            onConfirm = onQuit,
            // Back to the pause sheet, which is where the question was asked from.
            onDismiss = { showQuit = false; showSheet = true },
        )
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                         color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(72.dp).shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp)).background(color).clickable { onClick() }.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(8.dp))
        Text(label.uppercase(), color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
private fun PauseSheet(
    teamName: String, explaining: String, answering: String,
    correct: Int, skip: Int, onResume: () -> Unit, onStop: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(enabled = false) {}) {
        Column(
            Modifier.fillMaxWidth().systemBarsPadding().padding(16.dp)
                .shadow(20.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.playing).uppercase(), color = Alias.textSecondary,
                        fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(teamName, color = Alias.textPrimary, fontFamily = Alias.display,
                        fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("$correct ${stringResource(R.string.corrected)} · $skip ${stringResource(R.string.skipped)}",
                        color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                }
                TextButton(onClick = onStop) {
                    Text(stringResource(R.string.stop), color = Alias.danger, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.size(6.dp))
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Alias.success).clickable { onResume() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Alias.divider))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                PlayerColumn(stringResource(R.string.explaining), explaining, Modifier.weight(1f))
                PlayerColumn(stringResource(R.string.answering), answering, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlayerColumn(label: String, name: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = Alias.textSecondary, fontFamily = Alias.body,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/**
 * Alias Junior words are stored as "NNN-word" where NNN maps to a picture card
 * (res/raw/junior_NNN.svg, ported from the iOS junior asset set). Returns the clean
 * display word plus that raw-resource name, or null when the word isn't a junior
 * picture word — which is how every other deck is detected as "plain text".
 */
private fun juniorImageRes(context: Context, raw: String): Pair<String, String>? {
    val dash = raw.indexOf('-')
    if (dash != 3) return null
    val num = raw.substring(0, dash)
    if (!num.all { it.isDigit() }) return null
    val resName = "junior_$num"
    if (context.resources.getIdentifier(resName, "raw", context.packageName) == 0) return null
    return raw.substring(dash + 1) to resName
}

/**
 * Taboo words are stored as "target: forbidden1, forbidden2, …" — the player explains the
 * target without saying any forbidden word. Returns (target, forbidden list), or null when
 * the word isn't a taboo word (that's how every other deck is treated as plain text).
 */
private fun parseTaboo(raw: String): Pair<String, List<String>>? {
    val idx = raw.indexOf(": ")
    if (idx <= 0) return null
    val target = raw.substring(0, idx).trim()
    val forbidden = raw.substring(idx + 2).split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (target.isEmpty() || forbidden.size < 2) return null
    return target to forbidden
}

/** The Taboo card: the target word on an overlapping pill, above the forbidden words. */
/**
 * The clock, as its own disc. In camera mode it floats over the stage's corner rather than
 * sitting behind the word, because the stage is opaque and the faded number would be lost.
 */
@Composable
private fun TimerBadge(seconds: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(4.dp, Alias.brandBottom, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$seconds", color = Alias.brandBottom, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 27.sp,
        )
    }
}

@Composable
private fun TabooCard(target: String, forbidden: List<String>) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        // Forbidden words card (leaves top room for the overlapping target pill).
        Column(
            Modifier.fillMaxWidth().padding(top = 26.dp).clip(RoundedCornerShape(30.dp))
                .background(TabooCard).padding(top = 52.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            forbidden.forEach { w ->
                Text(w, color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp, textAlign = TextAlign.Center)
            }
        }
        // Target word pill, overlapping the top edge of the card.
        Box(
            Modifier.shadow(10.dp, RoundedCornerShape(26.dp), clip = false)
                .clip(RoundedCornerShape(26.dp)).background(TabooPill)
                .padding(horizontal = 28.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(target, color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Renders a res/raw SVG (the junior picture cards) to an [ImageBitmap], cached per name.
 *
 * The SVGs vary wildly (square 512s, tiny 64s, `pt` units, offset/negative viewBoxes), so we
 * normalise each into a square viewport (fit + centred via its own preserveAspectRatio), then
 * crop the transparent margins. That removes any asymmetric viewBox padding — the drawing ends
 * up tight and truly centred once Compose fits it, instead of clipped or shifted to one side.
 */
@Composable
private fun rememberSvgImage(resName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(resName) {
        runCatching {
            val id = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (id == 0) return@runCatching null
            context.resources.openRawResource(id).use { input ->
                val target = 768
                val svg = SVG.getFromInputStream(input).apply {
                    // Override the intrinsic width/height (may be in pt) so the viewBox maps
                    // into our pixel viewport regardless of the document's declared units.
                    setDocumentWidth(target.toFloat())
                    setDocumentHeight(target.toFloat())
                }
                val bmp = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
                svg.renderToCanvas(Canvas(bmp))
                trimTransparentBorders(bmp).asImageBitmap()
            }
        }.getOrNull()
    }
}

/** Crops fully-transparent rows/columns from every edge so the artwork sits centred and tight. */
private fun trimTransparentBorders(src: Bitmap): Bitmap {
    val w = src.width; val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)
    fun rowEmpty(y: Int): Boolean { val b = y * w; for (x in 0 until w) if (pixels[b + x] ushr 24 != 0) return false; return true }
    fun colEmpty(x: Int): Boolean { for (y in 0 until h) if (pixels[y * w + x] ushr 24 != 0) return false; return true }
    var top = 0; var bottom = h - 1; var left = 0; var right = w - 1
    while (top < bottom && rowEmpty(top)) top++
    while (bottom > top && rowEmpty(bottom)) bottom--
    while (left < right && colEmpty(left)) left++
    while (right > left && colEmpty(right)) right--
    val nw = right - left + 1; val nh = bottom - top + 1
    return if (nw <= 0 || nh <= 0 || (nw == w && nh == h)) src
    else Bitmap.createBitmap(src, left, top, nw, nh)
}
