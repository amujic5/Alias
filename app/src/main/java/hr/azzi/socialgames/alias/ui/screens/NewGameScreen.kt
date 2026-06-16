package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CardShape
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun NewGameScreen(
    teams: SnapshotStateList<Team>,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onStart: () -> Unit,
) {
    var editIndex by remember { mutableStateOf<Int?>(null) }   // null = closed
    var creating by remember { mutableStateOf(false) }

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.new_game).uppercase(), 22)
            }
            Overline(stringResource(R.string.teams).uppercase(), modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 8.dp))

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(teams.size) { i ->
                    TeamCard(
                        team = teams[i],
                        onToggle = { teams[i] = teams[i].copy(playing = !teams[i].playing); onChanged() },
                        onClick = { editIndex = i },
                    )
                }
                item {
                    CreateTeamButton { creating = true }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Bottom bar: VS line + start
            val vs = teams.filter { it.playing }.joinToString("  VS  ") { it.teamName }
            Column(
                Modifier.fillMaxWidth().shadow(20.dp).background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (vs.isNotEmpty()) {
                    Text(vs, color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
                PillButton(stringResource(R.string.start_the_game), PillKind.Primary, onClick = onStart)
            }
        }
    }

    // Create / edit dialog
    if (creating) {
        TeamDialog(team = null, onDismiss = { creating = false },
            onSave = { p1, p2, name -> teams.add(Team(p1, p2, name)); onChanged(); creating = false },
            onDelete = null)
    }
    editIndex?.let { idx ->
        val t = teams[idx]
        TeamDialog(team = t, onDismiss = { editIndex = null },
            onSave = { p1, p2, name ->
                teams[idx] = t.copy(firstPlayer = p1, secondPlayer = p2, teamName = name); onChanged(); editIndex = null
            },
            onDelete = { teams.removeAt(idx); onChanged(); editIndex = null })
    }
}

@Composable
private fun TeamCard(team: Team, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(10.dp, CardShape, clip = false).clip(CardShape)
            .background(Color.White).clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(team.teamName, color = Alias.textPrimary, fontFamily = Alias.display,
                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(listOf(team.firstPlayer, team.secondPlayer).filter { it.isNotBlank() }.joinToString(", "),
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
        }
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(50))
                .background(if (team.playing) Alias.accent else Color(0xFFE2E8F1))
                .clickable { onToggle() },
            contentAlignment = Alignment.Center,
        ) {
            if (team.playing) Icon(Icons.Filled.Check, null, tint = Alias.onAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CreateTeamButton(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(50))
            .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text("+ " + stringResource(R.string.create_team), color = Color.White,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun TeamDialog(
    team: Team?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(team?.teamName ?: "") }
    var p1 by remember { mutableStateOf(team?.firstPlayer ?: "") }
    var p2 by remember { mutableStateOf(team?.secondPlayer ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(p1.trim(), p2.trim(), name.trim()) }) {
                Text(stringResource(R.string.create), color = Alias.blue600, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = Alias.danger) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Alias.textSecondary) }
            }
        },
        title = { Text(stringResource(R.string.team_name), fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.team_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(p1, { p1 = it }, singleLine = true,
                    label = { Text(stringResource(R.string.player_1)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(p2, { p2 = it }, singleLine = true,
                    label = { Text(stringResource(R.string.player_2)) }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}
