package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.foundation.layout.width
import hr.azzi.socialgames.alias.ui.theme.BackButton
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.DisplayText

/**
 * The player's chosen AI mascot (Rive). Ported from iOS AIAvatar — each case maps
 * to a `.riv` file in res/raw (all share the artboard "avatar"). Persisted in the
 * "ai_settings" prefs under "ai_selected_avatar"; default [ROBOT].
 */
enum class AiAvatar(
    val rawRes: Int,
    val nameRes: Int,
    val tint: Color,
    /**
     * A still of the mascot, for what is drawn rather than animated — the shareable
     * result card. A Rive artboard cannot be turned into a bitmap off-screen, so the
     * same artwork also ships as a plain drawable.
     */
    val stillRes: Int,
) {
    ROBOT(R.raw.aliasbot, R.string.ai_avatar_robot, Color(0xFFE8EFFE), R.drawable.mascot_robot),
    WORDMONSTER(R.raw.wordmonster, R.string.ai_avatar_wordmonster, Color(0xFFE2FAF6), R.drawable.mascot_wordmonster),
    OWL(R.raw.aliasowl, R.string.ai_avatar_owl, Color(0xFFF0EAFF), R.drawable.mascot_owl),
    CAT(R.raw.catbot, R.string.ai_avatar_cat, Color(0xFFFFF0E6), R.drawable.mascot_cat);

    companion object {
        private const val PREFS = "ai_settings"
        private const val KEY = "ai_selected_avatar"

        fun save(context: Context, avatar: AiAvatar) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, avatar.name).apply()
        }

        fun current(context: Context): AiAvatar {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.name == raw } ?: ROBOT
        }
    }
}

/** Full-screen mascot chooser. Picking one persists it and returns via [onPick]. */
@Composable
fun AvatarPickerOverlay(
    selected: AiAvatar,
    onDismiss: () -> Unit,
    onPick: (AiAvatar) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0E1730)).clickable(false) {}) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onDismiss)
                Spacer(Modifier.width(10.dp))
                DisplayText(stringResource(R.string.ai_choose_avatar), 22)
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(AiAvatar.entries.toList()) { a ->
                    AvatarCard(a, a == selected) { onPick(a) }
                }
            }
        }
    }
}

@Composable
private fun AvatarCard(avatar: AiAvatar, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().height(180.dp).clip(shape).background(avatar.tint)
            .border(if (selected) 3.dp else 0.dp, if (selected) Alias.accent else Color.Transparent, shape)
            .clickable { onClick() }.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AvatarThumb(avatar.rawRes, Modifier.size(110.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(avatar.nameRes), color = Alias.textPrimary, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1,
        )
    }
}

/** A small tappable avatar thumbnail for a setup screen. */
@Composable
fun AvatarPill(onClick: () -> Unit) {
    val context = LocalContext.current
    val avatar = AiAvatar.current(context)
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(avatar.tint)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { AvatarThumb(avatar.rawRes, Modifier.size(40.dp)) }
}
