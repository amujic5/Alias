package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onHowToPlay: () -> Unit,
    onShare: () -> Unit,
    onAiChallenge: () -> Unit,
) {
    BrandBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                ScatteredSquares()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DisplayText("ALIAS", 56)
                    DisplayText("WORDS", 56)
                }
            }
            Spacer(Modifier.weight(1f))
            AiChallengeButton(onAiChallenge)
            Spacer(Modifier.height(12.dp))
            PillButton(stringResource(R.string.new_game).uppercase(), PillKind.Primary, onClick = onNewGame)
            Spacer(Modifier.height(12.dp))
            PillButton(stringResource(R.string.how_to_play).uppercase(), PillKind.Light, onClick = onHowToPlay)
            Spacer(Modifier.height(12.dp))
            PillButton(stringResource(R.string.tell_your_friends).uppercase(), PillKind.Light, onClick = onShare)
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Distinct teal→blue AI Challenge entry button. */
@Composable
private fun AiChallengeButton(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(56.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Brush.horizontalGradient(listOf(Color(0xFF2BC4A8), Color(0xFF0F70D1))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text("🤖 AI CHALLENGE", color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

/** Decorative floating rounded squares behind the logo (matches Figma home). */
@Composable
private fun ScatteredSquares() {
    val items = listOf(
        Triple(Alias.confetti[0], (-120).dp to (-90).dp, 44),
        Triple(Alias.confetti[1], (120).dp to (-70).dp, 36),
        Triple(Alias.confetti[2], (-130).dp to (60).dp, 40),
        Triple(Alias.confetti[3], (110).dp to (90).dp, 30),
        Triple(Alias.confetti[4], (-70).dp to (120).dp, 26),
        Triple(Alias.confetti[5], (80).dp to (-130).dp, 32),
    )
    items.forEachIndexed { i, (color, off, sz) ->
        Box(
            Modifier
                .offset(x = off.first, y = off.second)
                .size(sz.dp)
                .rotate((i * 17 % 30 - 15).toFloat())
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
    }
}
