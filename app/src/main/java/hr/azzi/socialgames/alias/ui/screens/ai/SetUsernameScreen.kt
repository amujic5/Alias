package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun SetUsernameScreen(
    busy: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var handle by remember { mutableStateOf("") }
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            DisplayText(stringResource(R.string.ai_pick_username), 28)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.ai_username_hint),
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontFamily = Alias.body)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it.filter { c -> c.isLetterOrDigit() }.take(15) },
                singleLine = true,
                label = { Text(stringResource(R.string.ai_username)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Alias.danger, fontFamily = Alias.body, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            if (busy) CircularProgressIndicator(color = Color.White)
            else PillButton(stringResource(R.string.ai_continue), PillKind.Primary, enabled = handle.length >= 3) { onSubmit(handle) }
        }
    }
}
