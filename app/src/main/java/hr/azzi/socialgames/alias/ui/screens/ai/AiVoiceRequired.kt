package hr.azzi.socialgames.alias.ui.screens.ai

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ai.AISttSupport
import hr.azzi.socialgames.alias.ui.theme.Alias
import java.util.Locale

private val Gold = Color(0xFFFFD23D)

/**
 * Blocking gate shown when none of the AI voices in [locales] is installed.
 * The game can't be played without one, so the only action is to download —
 * leaving is the screen's existing close (X) button.
 *
 * Every accepted voice is listed, not just the last one tried: Serbian and
 * Bosnian also play with a Croatian voice, and naming only Croatian to someone
 * who picked Serbian reads like the wrong language is being demanded.
 */
@Composable
fun VoiceRequiredBody(locales: List<Locale>, onDownload: () -> Unit) {
    val or = stringResource(R.string.ai_tts_voice_or)
    val language = locales
        .map { it.getDisplayLanguage(it).replaceFirstChar { c -> c.uppercaseChar() } }
        .reduceOrNull { a, b -> "$a $or $b" }
        .orEmpty()
    SpeechGateBody(
        emoji = "🔊",
        title = stringResource(R.string.ai_tts_voice_title),
        message = stringResource(R.string.ai_tts_voice_message, language),
        actionLabel = stringResource(R.string.ai_tts_voice_download),
        onAction = onDownload,
    )
}

/** Shared layout for the blocking speech gates (missing voice, missing recogniser).
 *  [actionLabel] null renders the screen without a button, for the cases where
 *  there is genuinely nothing the player can do about it. */
@Composable
fun SpeechGateBody(
    emoji: String,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                color = Gold, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
                fontSize = 23.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                message,
                color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body,
                fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(28.dp))
                Text(
                    actionLabel,
                    color = Color(0xFF0E1726), fontFamily = Alias.body, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Gold)
                        .clickable { onAction() }.padding(horizontal = 40.dp, vertical = 15.dp),
                )
            }
        }
    }
}

/** Blocking gate for speech *recognition*: no service, no network, or a language
 *  the recogniser doesn't offer. Mirrors the voice gate so the two read alike. */
@Composable
fun SttUnavailableBody(status: AISttSupport.Status, language: Locale, onRetry: () -> Unit) {
    val name = language.getDisplayLanguage(language).replaceFirstChar { it.uppercaseChar() }
    when (status) {
        AISttSupport.Status.NoService -> SpeechGateBody(
            emoji = "🎙️",
            title = stringResource(R.string.ai_stt_unavailable_title),
            message = stringResource(R.string.ai_stt_no_service_message),
        )
        AISttSupport.Status.NeedsInternet -> SpeechGateBody(
            emoji = "📶",
            title = stringResource(R.string.ai_stt_offline_title),
            message = stringResource(R.string.ai_stt_offline_message, name),
            actionLabel = stringResource(R.string.ai_stt_retry),
            onAction = onRetry,
        )
        AISttSupport.Status.LanguageUnsupported -> SpeechGateBody(
            emoji = "🎙️",
            title = stringResource(R.string.ai_stt_unavailable_title),
            message = stringResource(R.string.ai_stt_language_message, name),
        )
        AISttSupport.Status.Ready -> Unit
    }
}

/** Open the TTS engine's voice-data installer (or TTS settings as a fallback).
 *  Returns false if neither could be opened. */
fun launchVoiceInstaller(context: Context): Boolean =
    runCatching {
        context.startActivity(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess || runCatching {
        context.startActivity(
            Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess
