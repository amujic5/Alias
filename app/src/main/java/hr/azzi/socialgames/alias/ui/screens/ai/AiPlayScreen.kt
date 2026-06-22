package hr.azzi.socialgames.alias.ui.screens.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import hr.azzi.socialgames.alias.ai.AiPlayController
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText

private val Gold = Color(0xFFFFD23D)

@Composable
fun AiPlayScreen(
    config: AIPracticeConfig,
    fixedWords: List<String>? = null,
    onChallengeFinish: ((AIPracticeResult, List<String>) -> Unit)? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var sessionKey by remember { mutableIntStateOf(0) }
    val controller = remember(sessionKey) { AiPlayController(context, config, fixedWords, onChallengeFinish) }
    DisposableEffect(controller) { onDispose { controller.dispose() } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.start() else controller.denied()
    }
    LaunchedEffect(controller) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) controller.start() else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Re-check the voice whenever we come back (e.g. from the voice installer):
    // the voice is required every time, so re-verify rather than assume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                controller.phase == AiPlayController.Phase.VoiceRequired) {
                controller.runVoiceGate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (controller.phase == AiPlayController.Phase.Done && onChallengeFinish == null) {
        AiResultScreen(controller.result, title = stringResource(R.string.ai_practice_caps), onPlayAgain = { sessionKey++ }, onClose = onClose)
        return
    }
    if (controller.phase == AiPlayController.Phase.Done) {
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
                    AiPlayController.Phase.Loading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                    AiPlayController.Phase.VoiceRequired ->
                        controller.voiceMissing?.let { loc ->
                            VoiceRequiredBody(loc, onDownload = { launchVoiceInstaller(context) })
                        }
                    AiPlayController.Phase.Denied ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(stringResource(R.string.ai_mic_needed),
                                color = Color.White, textAlign = TextAlign.Center)
                        }
                    else -> Playing(controller)
                }
            }
            if (controller.flash) Box(Modifier.fillMaxSize().background(Alias.success.copy(alpha = 0.18f)))
            if (controller.penaltyFlash) {
                Box(Modifier.fillMaxSize().background(Color(0xFFFF3B30).copy(alpha = 0.22f)), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFFF3B30).copy(alpha = 0.92f))
                            .padding(horizontal = 26.dp, vertical = 20.dp)) {
                        DisplayText(stringResource(R.string.ai_said_word), 22)
                        Text(stringResource(R.string.ai_word_skipped), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Playing(c: AiPlayController) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ai_explain_dont_say), color = Color.White.copy(alpha = 0.6f), fontFamily = Alias.body,
            fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(14.dp))
        Text(c.word.uppercase(), color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
            fontSize = 44.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))

        Spacer(Modifier.height(36.dp))

        val explaining = c.turn == AiPlayController.Turn.Explaining
        Box(Modifier.size(180.dp).clickable(enabled = c.turn != AiPlayController.Turn.AiGuessing) { c.toggleExplain() },
            contentAlignment = Alignment.Center) {
            if (explaining) {
                Box(Modifier.size(168.dp).clip(CircleShape).background(Gold.copy(alpha = 0.18f)))
                Box(Modifier.size(128.dp).clip(CircleShape).border(3.dp, Gold, CircleShape))
            } else {
                Box(Modifier.size(124.dp).clip(CircleShape).dashedCircle(Color.White.copy(alpha = 0.55f)))
            }
            Box(Modifier.size(110.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
            Box(Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f)))
            Box(Modifier.size(66.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, null, tint = Alias.blue600, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        val status = when (c.turn) {
            AiPlayController.Turn.Idle -> if (c.aiGuess == null) stringResource(R.string.ai_tap_to_explain) else stringResource(R.string.ai_not_it_explain_more)
            AiPlayController.Turn.Explaining -> stringResource(R.string.ai_listening_tap_done)
            AiPlayController.Turn.AiGuessing -> stringResource(R.string.ai_ai_is_guessing)
        }
        DisplayText(status, 18, color = if (explaining) Gold else Color.White)

        if (explaining && c.transcript.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("“${c.transcript}”", color = Color.White.copy(alpha = 0.75f), fontFamily = Alias.body,
                fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
        c.aiGuess?.let { g ->
            Spacer(Modifier.height(16.dp))
            Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ai_ai_guesses), color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.size(8.dp))
                Text(g, color = Alias.accent, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("✓ ${c.correctWords.size}", color = Alias.success, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("» ${c.skippedWords.size}", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ai_skip_word), color = Color.White.copy(alpha = 0.9f), fontFamily = Alias.body, fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp, modifier = Modifier.clickable { c.skip() }.padding(8.dp))
    }
}

@Composable
private fun RingTimer(remaining: Double, total: Int) {
    val frac = if (total > 0) (remaining / total).toFloat().coerceIn(0f, 1f) else 0f
    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            drawArc(Color.White.copy(alpha = 0.25f), -90f, 360f, false,
                style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2))
            drawArc(Gold, -90f, 360f * frac, false,
                style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
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
