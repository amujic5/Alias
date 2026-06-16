package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun WinnerScreen(winnerName: String, onShare: () -> Unit, onFinish: () -> Unit) {
    BrandBackground {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🏆", fontSize = 96.sp)
            Spacer(Modifier.height(20.dp))
            DisplayText(stringResource(R.string.winner), 34, color = Alias.accent)
            Spacer(Modifier.height(8.dp))
            Text(winnerName, color = androidx.compose.ui.graphics.Color.White,
                fontFamily = Alias.display, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                fontSize = 28.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            PillButton(stringResource(R.string.share), PillKind.Light, onClick = onShare)
            Spacer(Modifier.height(14.dp))
            PillButton(stringResource(R.string.finish), PillKind.Primary, onClick = onFinish)
        }
    }
}
