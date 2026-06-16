package hr.azzi.socialgames.alias.ui.screens.ai

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ai.AIDeck
import hr.azzi.socialgames.alias.ai.AIDeckCatalog
import hr.azzi.socialgames.alias.ai.AILanguage
import hr.azzi.socialgames.alias.ai.AIMode
import hr.azzi.socialgames.alias.ai.AIPracticeConfig
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CCard
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun AiSetupScreen(
    title: String,
    subtitle: String,
    startLabel: String,
    onBack: () -> Unit,
    onStart: (AIPracticeConfig) -> Unit,
) {
    val context = LocalContext.current
    var deck by remember { mutableStateOf(AIDeckCatalog.decks.first()) }
    var language by remember { mutableStateOf(deck.languages.first()) }
    var seconds by remember { mutableStateOf(60) }
    var mode by remember { mutableStateOf(AIMode.AI_EXPLAINS) }
    var showPicker by remember { mutableStateOf(false) }

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(title, 24)
            }
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 14.sp,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp))

            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Mode selection
                ModeCard("Y", stringResource(R.string.ai_mode_you_explain), stringResource(R.string.ai_mode_you_explain_sub), Color(0xFFB9A7F6),
                    selected = mode == AIMode.YOU_EXPLAIN) { mode = AIMode.YOU_EXPLAIN }
                ModeCard("AI", stringResource(R.string.ai_mode_ai_explains), stringResource(R.string.ai_mode_ai_explains_sub), Color(0xFF2BC4A8),
                    selected = mode == AIMode.AI_EXPLAINS) { mode = AIMode.AI_EXPLAINS }

                CCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Deck row
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ai_deck), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Row(Modifier.clip(RoundedCornerShape(50)).background(Alias.fieldBg).clickable { showPicker = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(deckName(deck), color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Icon(Icons.Filled.ChevronRight, null, tint = Alias.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Divider()
                        // Language
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ai_language), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Segmented(deck.languages.map { it.display }, deck.languages.indexOf(language)) { i ->
                                language = deck.languages[i]
                            }
                        }
                        Divider()
                        // Total time
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ai_total_time), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            val opts = listOf(40, 60, 80, 100)
                            Segmented(opts.map { "${it}s" }, opts.indexOf(seconds)) { i -> seconds = opts[i] }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                PillButton(startLabel, PillKind.Primary) {
                    onStart(AIPracticeConfig(deck = deck, language = language, totalSeconds = seconds, mode = mode))
                }
            }
        }
    }

    if (showPicker) {
        DeckPickerOverlay(
            selected = deck,
            onDismiss = { showPicker = false },
            onPick = { d ->
                deck = d
                if (!d.languages.contains(language)) language = d.languages.first()
                showPicker = false
            },
        )
    }
}

@Composable
private fun ModeCard(badgeLabel: String, title: String, subtitle: String, badge: Color, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) Modifier.border(2.dp, Alias.accent, RoundedCornerShape(20.dp)) else Modifier
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().then(border)
        .clip(RoundedCornerShape(20.dp)).background(Color.White).clickable { onClick() }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(badge),
                contentAlignment = Alignment.Center) { Text(badgeLabel, color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(subtitle, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
            }
            androidx.compose.foundation.layout.Box(Modifier.size(22.dp).clip(RoundedCornerShape(50))
                .background(if (selected) Alias.accent else Color(0xFFE2E8F1)))
        }
    }
}

@Composable
private fun deckName(deck: AIDeck): String {
    val ctx = LocalContext.current
    val id = ctx.resources.getIdentifier(deck.nameRes, "string", ctx.packageName)
    return if (id != 0) ctx.getString(id) else deck.id
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Alias.divider))
}

@Composable
private fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Alias.fieldBg).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(Modifier.clip(RoundedCornerShape(50)).background(if (sel) Alias.accent else Color.Transparent)
                .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(label, color = if (sel) Alias.onAccent else Alias.textSecondary, fontFamily = Alias.body,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DeckPickerOverlay(selected: AIDeck, onDismiss: () -> Unit, onPick: (AIDeck) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(false) {}) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.ai_choose_deck), 22)
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(AIDeckCatalog.decks) { d -> DeckCard(d, d.id == selected.id) { onPick(d) } }
            }
        }
    }
}

@Composable
private fun DeckCard(deck: AIDeck, selected: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val imgId = remember(deck.imageRes) { ctx.resources.getIdentifier(deck.imageRes, "drawable", ctx.packageName) }
    Column(
        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
            .clickable { onClick() }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (imgId != 0) Image(painterResource(imgId), null, Modifier.size(64.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(12.dp))
        Text(deckName(deck), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}
