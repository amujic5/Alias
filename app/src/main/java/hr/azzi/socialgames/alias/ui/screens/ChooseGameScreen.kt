package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CardShape
import hr.azzi.socialgames.alias.ui.theme.DisplayText

@Composable
fun ChooseGameScreen(
    decks: List<BoardGame>,
    onBack: () -> Unit,
    onPick: (BoardGame) -> Unit,
) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                DisplayText(stringResource(R.string.choose_game), 22)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(decks) { deck -> DeckCard(deck) { onPick(deck) } }
            }
        }
    }
}

@Composable
private fun DeckCard(deck: BoardGame, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val imgId = remember(deck.image) { ctx.resources.getIdentifier(deck.image, "drawable", ctx.packageName) }
    val nameId = remember(deck.name) { ctx.resources.getIdentifier(deck.name, "string", ctx.packageName) }
    // The tile is a fixed 150dp so the grid stays even. Padding of 12 top and 10 bottom
    // leaves 128dp of content, and the budget spends 118 — 8 nudge + 70 art + 4 gap + two
    // 18dp lines — so the column centres the 10dp left over. Anything that pushes past 128
    // costs the second line of the name, which is cut off mid-glyph rather than dropped. Anything that pushes past 126 costs the second
    // line of the name, which is cut off mid-glyph rather than dropped.
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(12.dp, CardShape, clip = false)
            .clip(CardShape)
            .background(Color.White)
            .clickable { onClick() },
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Nudged down rather than centred on its own: the artwork reads as sitting a
        // little low in the tile, which is what leaves the name room to breathe.
        Spacer(Modifier.height(8.dp))
        if (imgId != 0) {
            Image(painterResource(imgId), contentDescription = null,
                modifier = Modifier.size(width = 90.dp, height = 70.dp),
                contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (nameId != 0) stringResource(nameId) else deck.name,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
            lineHeight = 18.sp,
            color = Alias.textPrimary, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }

        // Opposite corners, not a stack: the two badges say different kinds of thing,
        // and side by side a deck that is both stops reading as one long label.
        if (deck.isNew) {
            DeckBadge(
                text = stringResource(R.string.deck_badge_new),
                background = Color(0xFFFF4D8D),
                ink = Color.White,
                crown = false,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            )
        }
        if (!deck.free) {
            DeckBadge(
                text = stringResource(R.string.deck_badge_premium),
                background = Alias.accent,
                // Dark amber rather than black: on a yellow pill this reads as part of
                // the badge instead of text dropped on top of it.
                ink = Color(0xFF5B4200),
                crown = true,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

/**
 * The small pill in a deck tile's corner.
 *
 * PREMIUM is derived from `free`, so a deck cannot be sold without wearing it. NEW is a
 * hand-kept list on BoardGame, because nothing in the data knows when a deck stops being
 * new. See the iOS DeckBadgeView — same two rules, same colours.
 */
@Composable
private fun DeckBadge(
    text: String,
    background: Color,
    ink: Color,
    crown: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (crown) {
            Image(
                painterResource(R.drawable.ic_crown),
                contentDescription = null,
                modifier = Modifier.size(9.dp),
            )
            Spacer(Modifier.width(3.dp))
        }
        Text(
            text,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp, lineHeight = 11.sp, color = ink,
            maxLines = 1,
        )
    }
}
