package hr.azzi.socialgames.alias.ui.screens.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ai.AIPracticeConfig
import hr.azzi.socialgames.alias.ai.AIPracticeResult
import hr.azzi.socialgames.alias.ai.AiExplainController
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText

private val Gold = Color(0xFFFFD23D)

@Composable
fun AiExplainScreen(
    config: AIPracticeConfig,
    fixedWords: List<String>? = null,
    onChallengeFinish: ((AIPracticeResult, List<String>) -> Unit)? = null,
    /** Clues the Arena's server dealt with the words. See `AiExplainController`. */
    clues: Map<String, List<String>>? = null,
    /** Set by the Arena, whose round is already running. */
    deadlineMs: Long? = null,
    /** Which ad unit this round is worth. See `AdInterstitial`. */
    ads: hr.azzi.socialgames.alias.ai.AdInterstitial = hr.azzi.socialgames.alias.ai.AdInterstitial.ai,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var sessionKey by remember { mutableIntStateOf(0) }
    val controller = remember(sessionKey) {
        AiExplainController(context, config, fixedWords, onChallengeFinish, clues, deadlineMs, ads)
    }
    DisposableEffect(controller) { onDispose { controller.dispose() } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.start() else controller.denied()
    }
    LaunchedEffect(controller) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) controller.start() else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Coming back to the foreground while the prompt is up means the player has
    // just been to the installer: reconnect to the TTS engine and re-check, so a
    // voice they installed a moment ago is picked up without leaving the screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                (controller.phase == AiExplainController.Phase.VoiceRequired ||
                    controller.phase == AiExplainController.Phase.SttUnavailable)) {
                controller.retrySpeechGates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (controller.phase == AiExplainController.Phase.Done && onChallengeFinish == null) {
        AiResultScreen(controller.result, title = stringResource(R.string.ai_practice_caps), onPlayAgain = { sessionKey++ }, onClose = onClose)
        return
    }
    if (controller.phase == AiExplainController.Phase.Done) {
        // challenge mode: host navigates away via onChallengeFinish
        BrandBackground { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
        return
    }

    BrandBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, tint = Color.White) }
                    Spacer(Modifier.weight(1f))
                    RingTimer(controller.remaining, config.totalSeconds)
                }

                when (controller.phase) {
                    AiExplainController.Phase.Loading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                    AiExplainController.Phase.VoiceRequired ->
                        VoiceRequiredBody(controller.voiceMissing, onDownload = { launchVoiceInstaller(context) })
                    AiExplainController.Phase.SttUnavailable ->
                        SttUnavailableBody(
                            controller.sttStatus, config.language.javaLocale,
                            onRetry = { controller.retrySpeechGates() },
                        )
                    AiExplainController.Phase.Denied ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(stringResource(R.string.ai_mic_needed),
                                color = Color.White, textAlign = TextAlign.Center, fontFamily = Alias.body)
                        }
                    else -> Guessing(controller)
                }
            }
            if (controller.flash) {
                Box(Modifier.fillMaxSize().background(Alias.success.copy(alpha = 0.18f)))
            }
        }
    }
}

@Composable
private fun Guessing(c: AiExplainController) {
    // Animated AI bot: talks while it speaks the clue, listens while you guess,
    // thinks while preparing the next clue, and celebrates a correct answer.
    val bot = rememberBotController()
    LaunchedEffect(c.listening, c.speaking, c.phase) {
        bot.mood = when {
            c.listening -> BotMood.Listening
            c.speaking -> BotMood.Talking
            c.phase == AiExplainController.Phase.Thinking -> BotMood.Thinking
            else -> BotMood.Idle
        }
    }
    LaunchedEffect(c.correctWords.size) {
        if (c.correctWords.size > 0) bot.react(BotReaction.Correct)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val s = rememberCompactScale(maxHeight)
    Column(
        Modifier.fillMaxSize().padding(top = 10.dp.compact(s)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(6.dp.compact(s)))
        Text(stringResource(R.string.ai_ai_clue), color = Color.White.copy(alpha = 0.6f), fontFamily = Alias.body,
            fontWeight = FontWeight.Black, fontSize = 12.sp.compact(s), letterSpacing = 3.sp)
        Spacer(Modifier.height(14.dp.compact(s)))
        AutoShrinkText(
            text = if (c.clue.isEmpty()) "…" else "“${c.clue}”",
            maxFontSize = 23.sp.compact(s),
            maxLines = 3,
            color = Color.White,
            fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold,
            lineHeightRatio = 1.3f,
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        Spacer(Modifier.height(18.dp.compact(s)))

        // Status pill
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background((if (c.listening) Gold else Color.White).copy(alpha = if (c.listening) 0.20f else 0.14f))
                .padding(horizontal = 14.dp.compact(s), vertical = 8.dp.compact(s)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (c.listening) stringResource(R.string.ai_listening) else stringResource(R.string.ai_ai_speaking),
                color = if (c.listening) Gold else Color.White, fontFamily = Alias.body,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp.compact(s))
        }
        Spacer(Modifier.height(16.dp.compact(s)))
        BotAvatar(bot, Modifier.size(180.dp.compact(s)))
        Spacer(Modifier.height(12.dp.compact(s)))

        // Mic orb
        Box(
            Modifier.size(180.dp.compact(s)).clickable { c.answerNow() },
            contentAlignment = Alignment.Center,
        ) {
            if (c.listening) {
                Box(Modifier.size(168.dp.compact(s)).clip(CircleShape).background(Gold.copy(alpha = 0.18f)))
                Box(Modifier.size(128.dp.compact(s)).clip(CircleShape).border(3.dp, Gold, CircleShape))
            } else {
                Box(Modifier.size(124.dp.compact(s)).clip(CircleShape)
                    .dashedCircle(Color.White.copy(alpha = 0.55f)))
            }
            Box(Modifier.size(110.dp.compact(s)).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
            Box(Modifier.size(88.dp.compact(s)).clip(CircleShape).background(Color.White.copy(alpha = 0.20f)))
            Box(Modifier.size(66.dp.compact(s)).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, null, tint = Alias.blue600, modifier = Modifier.size(28.dp.compact(s)))
            }
        }

        Spacer(Modifier.height(14.dp.compact(s)))
        DisplayText(
            if (c.listening) stringResource(R.string.ai_youre_guessing) else stringResource(R.string.ai_tap_mic_answer),
            18.compactSp(s),
        )
        if (c.listening && c.transcript.isNotEmpty()) {
            Spacer(Modifier.height(10.dp.compact(s)))
            Text(c.transcript, color = Gold, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp.compact(s), maxLines = 1)
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp.compact(s))) {
            Chip(stringResource(R.string.ai_hear_again), s) { c.hearAgain() }
            Chip(stringResource(R.string.ai_new_clue), s) { c.newClue() }
        }
        Spacer(Modifier.height(12.dp.compact(s)))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("✓ ${c.correctWords.size}", color = Alias.success, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp.compact(s))
            Text("» ${c.skippedWords.size}", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp.compact(s))
        }
        Spacer(Modifier.height(8.dp.compact(s)))
        Text(stringResource(R.string.ai_skip_word), color = Color.White.copy(alpha = 0.9f), fontFamily = Alias.body,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp.compact(s),
            modifier = Modifier.clickable { c.reveal() }.padding(8.dp.compact(s)))
    }
    }
}

@Composable
private fun Chip(title: String, s: Float = 1f, onClick: () -> Unit) {
    Text(title, color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp.compact(s),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 11.dp))
}

@Composable
private fun RingTimer(remaining: Double, total: Int) {
    val frac by animateFloatAsState(
        targetValue = if (total > 0) (remaining / total).toFloat().coerceIn(0f, 1f) else 0f, label = "ring")
    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            drawArc(Color.White.copy(alpha = 0.25f), -90f, 360f, false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2))
            drawArc(Gold, -90f, 360f * frac, false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2))
        }
        Text("${remaining.toInt().coerceAtLeast(0)}", color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

private fun Modifier.dashedCircle(color: Color): Modifier = this.then(
    Modifier.drawBehind {
        drawCircle(color = color, style = Stroke(width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 18f))))
    }
)
