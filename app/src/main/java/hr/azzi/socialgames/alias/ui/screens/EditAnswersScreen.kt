package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun EditAnswersScreen(
    teamName: String,
    words: SnapshotStateList<MarkedWord>,
    onToggle: (Int, Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val correct = words.count { it.isCorrect }
    val skip = words.size - correct

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // Blue header
        Box(Modifier.fillMaxWidth().background(Alias.brandGradient)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, top = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, null, tint = Color.White) }
                DisplayText(stringResource(R.string.edit_answers).uppercase(), 22)
            }
        }

        Column(Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp)) {
            Overline(stringResource(R.string.this_round), color = Alias.textSecondary)
            Text(teamName, color = Alias.textPrimary, fontFamily = Alias.display,
                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text("$correct ${stringResource(R.string.corrected)} · $skip ${stringResource(R.string.skipped)}",
                color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(words) { word ->
                val i = words.indexOf(word)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(word.word, color = if (word.isCorrect) Alias.textPrimary else Alias.danger,
                        fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.weight(1f))
                    Switch(
                        checked = word.isCorrect,
                        onCheckedChange = { onToggle(i, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = Alias.success,
                            uncheckedThumbColor = Color.White, uncheckedTrackColor = Alias.divider,
                            uncheckedBorderColor = Color.Transparent),
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 20.dp).background(Alias.divider))
            }
        }

        Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f)) { PillButton(stringResource(R.string.cancel), PillKind.Dark, onClick = onCancel) }
            Box(Modifier.weight(1f)) { PillButton(stringResource(R.string.save), PillKind.Primary, onClick = onSave) }
        }
    }
}
