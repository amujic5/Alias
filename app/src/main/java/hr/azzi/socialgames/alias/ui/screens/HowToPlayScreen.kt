package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun HowToPlayScreen(onGotIt: () -> Unit) {
    BrandBackground {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.how_to_play), color = Alias.textPrimary,
                    fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))
                        .background(Alias.fieldBg),
                    contentAlignment = Alignment.Center,
                ) { Text("👥", fontSize = 64.sp) }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.how_to_play_info), color = Alias.textSecondary,
                    fontFamily = Alias.body, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PillButton(stringResource(R.string.got_it), PillKind.Primary, onClick = onGotIt)
            }
        }
    }
}
