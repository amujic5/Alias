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
import hr.azzi.socialgames.alias.ui.theme.Alias
import java.util.Locale

private val Gold = Color(0xFFFFD23D)

/**
 * Blocking gate shown when the AI voice (TTS) for [locale] isn't installed.
 * The game can't be played without it, so the only action is to download —
 * leaving is the screen's existing close (X) button.
 */
@Composable
fun VoiceRequiredBody(locale: Locale, onDownload: () -> Unit) {
    val language = locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercaseChar() }
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔊", fontSize = 40.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.ai_tts_voice_title),
                color = Gold, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
                fontSize = 23.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.ai_tts_voice_message, language),
                color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body,
                fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.ai_tts_voice_download),
                color = Color(0xFF0E1726), fontFamily = Alias.body, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Gold)
                    .clickable { onDownload() }.padding(horizontal = 40.dp, vertical = 15.dp),
            )
        }
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
