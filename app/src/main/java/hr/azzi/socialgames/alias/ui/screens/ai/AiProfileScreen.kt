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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
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
    onDeleteAccount: () -> Unit,
    onClose: () -> Unit,
    /**
     * The Arena's half of this profile, when there is one.
     *
     * One profile, whichever door you came through. iOS briefly had two describing the
     * same person and they disagreed about the avatar; null here simply means the caller
     * has no Arena state to show, not that a second profile exists.
     */
    arena: hr.azzi.socialgames.alias.arena.ArenaProfileStore? = null,
    onPickAvatar: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
    onOpenStandings: () -> Unit = {},
) {
    var confirmDelete by remember { mutableStateOf(false) }
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.ai_profile), 22)
            }
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // The chosen avatar, not an initial: it is picked here, worn on every
                // Arena board, and a letter would be the one place it was missing.
                if (arena != null) {
                    hr.azzi.socialgames.alias.arena.ui.ArenaProfileIdentity(
                        profile = arena,
                        onPickAvatar = onPickAvatar,
                        onOpenRewards = onOpenRewards,
                    )
                } else {
                    Avatar(username, Alias.accent, 92)
                }
                Text("@$username", color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)

                if (arena != null) {
                    hr.azzi.socialgames.alias.arena.ui.ArenaProfileRanking(
                        profile = arena,
                        onOpenStandings = onOpenStandings,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(stringResource(R.string.ai_wins), stats.wins, Alias.success, Modifier.weight(1f))
                    Stat(stringResource(R.string.ai_losses), stats.losses, Alias.danger, Modifier.weight(1f))
                    Stat(stringResource(R.string.ai_draws), stats.draws, Color(0xFF8A93A6), Modifier.weight(1f))
                }

                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) { Overline(stringResource(R.string.ai_recent_challenges)) }
                    CCard(Modifier.fillMaxWidth(), padding = 0) {
                        Column {
                            recent.forEachIndexed { i, ch ->
                                Row(Modifier.fillMaxWidth().clickable { onOpenChallenge(ch) }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    val amCreator = ch.creatorId == uid
                                    Avatar(if (amCreator) "You" else ch.creatorName, Alias.blue600, 36)
                                    Spacer(Modifier.width(12.dp))
                                    Text(if (amCreator) stringResource(R.string.ai_your_challenge) else stringResource(R.string.ai_vs_name, ch.creatorName),
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
                PillButton(stringResource(R.string.ai_sign_out), PillKind.Light, onClick = onSignOut)
                Text(
                    stringResource(R.string.ai_delete_account),
                    color = Alias.danger,
                    fontFamily = Alias.body,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { confirmDelete = true }.padding(8.dp),
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.ai_delete_account_title)) },
            text = { Text(stringResource(R.string.ai_delete_account_msg)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteAccount() }) {
                    Text(stringResource(R.string.ai_delete_account_confirm), color = Alias.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.ai_delete_account_cancel)) }
            },
        )
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
