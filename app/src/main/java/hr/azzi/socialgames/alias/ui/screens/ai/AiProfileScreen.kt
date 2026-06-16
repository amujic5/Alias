package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.ai.AIChallenge
import hr.azzi.socialgames.alias.ai.AIUserStats
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiProfileScreen(
    username: String,
    uid: String,
    stats: AIUserStats,
    recent: List<AIChallenge>,
    onOpenChallenge: (AIChallenge) -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText("Profile", 22)
            }
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(username, Alias.accent, 92)
                Text("@$username", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("WINS", stats.wins, Alias.success, Modifier.weight(1f))
                    Stat("LOSSES", stats.losses, Alias.danger, Modifier.weight(1f))
                    Stat("DRAWS", stats.draws, Color(0xFF8A93A6), Modifier.weight(1f))
                }

                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) { Overline("RECENT CHALLENGES") }
                    CCard(Modifier.fillMaxWidth(), padding = 0) {
                        Column {
                            recent.forEachIndexed { i, ch ->
                                Row(Modifier.fillMaxWidth().clickable { onOpenChallenge(ch) }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    val amCreator = ch.creatorId == uid
                                    Avatar(if (amCreator) "You" else ch.creatorName, Alias.blue600, 36)
                                    Spacer(Modifier.width(12.dp))
                                    Text(if (amCreator) "Your challenge" else "vs ${ch.creatorName}",
                                        color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp, modifier = Modifier.weight(1f))
                                    Text(ch.deckName, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 12.sp)
                                }
                                if (i != recent.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp)
                                    .padding(horizontal = 16.dp).background(Alias.divider))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                PillButton("Sign out", PillKind.Light, onClick = onSignOut)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = color, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Text(label, color = Alias.textSecondary, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}
