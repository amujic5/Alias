package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
    // The tile is a fixed 150dp so the grid stays even, which leaves 126dp of content once
    // the padding is off: 66 art + 10 gap + two 18dp lines fits, where the design's 70 + 12
    // + a default line height did not, and the second line was cut off mid-glyph.
    Column(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(12.dp, CardShape, clip = false)
            .clip(CardShape)
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (imgId != 0) {
            Image(painterResource(imgId), contentDescription = null,
                modifier = Modifier.size(66.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (nameId != 0) stringResource(nameId) else deck.name,
            fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
            lineHeight = 18.sp,
            color = Alias.textPrimary, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}
