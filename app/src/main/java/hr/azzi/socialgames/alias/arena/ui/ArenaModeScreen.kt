package hr.azzi.socialgames.alias.arena.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ai.AILanguage
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaMode
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.arena.ArenaStore
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.PillButton

/**
 * Which half of the Arena, and in which language.
 *
 * Android port of iOS `ArenaModeView.swift`. The two decisions a player makes before they
 * ever see a round, and the only ones — everything after this is the clock.
 */
@Composable
fun ArenaModeScreen(
    mode: ArenaMode,
    lang: String,
    onMode: (ArenaMode) -> Unit,
    onLanguage: (String) -> Unit,
    onEnter: () -> Unit,
    onBack: () -> Unit,
) {
    var pickingLanguage by remember { mutableStateOf(false) }
    val language = remember(lang) {
        AILanguage.entries.firstOrNull { it.fileCode == lang } ?: AILanguage.EN
    }

    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.ChevronLeft, null, tint = Color.White) }
                Text(
                    stringResource(R.string.arena_mode_title),
                    fontFamily = Alias.display, fontSize = 26.sp, color = Color.White, maxLines = 1,
                )
            }

            Text(
                stringResource(R.string.arena_mode_subtitle),
                fontFamily = Alias.body, fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
            )

            ModeCard(
                mode = ArenaMode.GUESS,
                selected = mode == ArenaMode.GUESS,
                badge = stringResource(R.string.arena_mode_badge_ai),
                onSelect = { onMode(ArenaMode.GUESS) },
            )
            ModeCard(
                mode = ArenaMode.CLUE,
                selected = mode == ArenaMode.CLUE,
                badge = stringResource(R.string.arena_mode_badge_you),
                onSelect = { onMode(ArenaMode.CLUE) },
            )

            ArenaCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        ArenaSectionLabel(stringResource(R.string.arena_mode_language_label))
                        Text(
                            "${language.emoji}  ${language.aiName}",
                            fontFamily = Alias.display, fontSize = 20.sp,
                            color = ArenaColors.title, maxLines = 1,
                        )
                    }
                    Text(
                        stringResource(R.string.arena_mode_language_change),
                        fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 13.sp,
                        color = Alias.blue600, maxLines = 1,
                        modifier = Modifier.clickable { pickingLanguage = true }.padding(6.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            PillButton(title = stringResource(R.string.arena_mode_enter), onClick = onEnter)
        }
    }

    if (pickingLanguage) {
        ArenaLanguageSheet(
            current = lang,
            onPick = { onLanguage(it); pickingLanguage = false },
            onClose = { pickingLanguage = false },
        )
    }
}

@Composable
private fun ModeCard(mode: ArenaMode, selected: Boolean, badge: String, onSelect: () -> Unit) {
    ArenaCard(
        Modifier
            .fillMaxWidth()
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Alias.accent else ArenaColors.border,
                RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onSelect),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(ArenaPalette.modeGlyph),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (mode == ArenaMode.GUESS) Icons.Filled.QuestionMark else Icons.Filled.Mic,
                    null, tint = Color.White, modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(mode.titleRes),
                    fontFamily = Alias.display, fontSize = 20.sp, color = ArenaColors.title, maxLines = 1,
                )
                Text(
                    stringResource(mode.subtitleRes),
                    fontFamily = Alias.body, fontSize = 13.sp, color = ArenaColors.body, maxLines = 2,
                )
            }
            if (selected) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Alias.accent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, null, tint = Alias.onAccent, modifier = Modifier.size(15.dp)) }
            } else {
                Text(
                    badge,
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 10.sp,
                    color = ArenaColors.body, maxLines = 1,
                )
            }
        }
    }
}

/**
 * The languages the Arena runs rounds in.
 *
 * Not every language the app speaks: a language is only here when a deck was written for
 * it, and Bosnian has no clue file so no deck exists. Offering it would be a room nobody
 * is ever in.
 */
@Composable
private fun ArenaLanguageSheet(current: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ArenaColors.card)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.arena_mode_language_title),
                fontFamily = Alias.display, fontSize = 20.sp, color = ArenaColors.title,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(8.dp))
            ArenaStore.LANGUAGES.forEach { code ->
                val l = AILanguage.entries.firstOrNull { it.fileCode == code } ?: return@forEach
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { onPick(code) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(l.emoji, fontSize = 20.sp)
                    Text(
                        l.aiName,
                        fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = ArenaColors.title, maxLines = 1, modifier = Modifier.weight(1f),
                    )
                    if (code == current) {
                        Icon(Icons.Filled.Check, null, tint = Alias.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
