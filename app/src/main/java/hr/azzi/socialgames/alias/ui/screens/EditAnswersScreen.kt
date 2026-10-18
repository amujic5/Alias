package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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

/** How a row shows what its word does to the score. */
enum class PointsStyle {
    /** Every word is a flat point; a column of ones says nothing. */
    Hidden,
    /** Easy Medium Hard: the word's value, greyed out when skipped. */
    Value,
    /**
     * After Dark: what the row does to the total — `+1`, `+2` from CLIMAX, or `−1` for a
     * skip, since a skip costs the same whichever segment it came from.
     */
    Signed,
}

@Composable
fun EditAnswersScreen(
    teamName: String,
    words: SnapshotStateList<MarkedWord>,
    onToggle: (Int, Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    /** Whether, and how, a row shows what its word is worth. */
    pointsStyle: PointsStyle = PointsStyle.Hidden,
) {
    // Read off the edits being made here, not off the game: the round is only written back
    // on Save, and the tally has to follow the switches in the meantime.
    val correct = words.filter { it.isCorrect }.sumOf { it.points }
    val skip = words.count { !it.isCorrect }

    Column(Modifier.fillMaxSize().background(Alias.surface)) {
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
                    when (pointsStyle) {
                        PointsStyle.Hidden -> Unit
                        PointsStyle.Value -> {
                            // A skipped word keeps its value on screen but greys out — it is
                            // not being scored.
                            PointsBadge(word.points, dimmed = !word.isCorrect)
                            Spacer(Modifier.width(12.dp))
                        }
                        PointsStyle.Signed -> {
                            DeltaBadge(if (word.isCorrect) word.points else -1)
                            Spacer(Modifier.width(12.dp))
                        }
                    }
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

        Row(Modifier.fillMaxWidth().background(Alias.surface).navigationBarsPadding().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f)) { PillButton(stringResource(R.string.cancel), PillKind.Dark, onClick = onCancel) }
            Box(Modifier.weight(1f)) { PillButton(stringResource(R.string.save), PillKind.Primary, onClick = onSave) }
        }
    }
}
