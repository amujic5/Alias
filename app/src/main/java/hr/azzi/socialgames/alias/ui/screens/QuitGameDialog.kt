package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * "Are you sure that you want to quit the game?"
 *
 * One definition, used from every screen that can drop out of a game, so the wording and the
 * weight of the two buttons cannot drift apart between them.
 */
@Composable
fun QuitGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.warning),
                fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
            )
        },
        text = { Text(stringResource(R.string.are_you_sure_quit), fontFamily = Alias.body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes), color = Alias.danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no), color = Alias.textSecondary)
            }
        },
    )
}
