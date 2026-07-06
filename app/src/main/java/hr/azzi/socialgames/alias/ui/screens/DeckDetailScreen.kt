package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.Billing
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.serializedName
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.CardShape
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

/**
 * Deck detail — cover, description, age/players/time. Free (or already-owned) decks
 * show PLAY; locked paid decks show UNLOCK {price} that launches Google Play billing.
 * Mirrors the iOS BoardGameInfo screen.
 */
@Composable
fun DeckDetailScreen(
    deck: BoardGame,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onBuy: () -> Unit,
    onTryFree: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val unlockedIds by Billing.unlocked.collectAsState()
    val prices by Billing.prices.collectAsState()

    val productId = deck.id.serializedName()
    val unlocked = deck.free || unlockedIds.contains(productId)
    val price = prices[productId]

    val imgId = remember(deck.image) { ctx.resources.getIdentifier(deck.image, "drawable", ctx.packageName) }
    val nameId = remember(deck.name) { ctx.resources.getIdentifier(deck.name, "string", ctx.packageName) }
    val infoId = remember(deck.info) { ctx.resources.getIdentifier(deck.info, "string", ctx.packageName) }
    val title = (if (nameId != 0) stringResource(nameId) else deck.name).uppercase()
    val info = if (infoId != 0) stringResource(infoId) else ""

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                DisplayText(title, 22)
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.size(150.dp).shadow(16.dp, CardShape, clip = false).clip(CardShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    if (imgId != 0) {
                        Image(painterResource(imgId), contentDescription = null,
                            modifier = Modifier.size(96.dp), contentScale = ContentScale.Fit)
                    }
                }
                Spacer(Modifier.height(22.dp))
                if (info.isNotEmpty()) {
                    Text(
                        info,
                        color = Color.White.copy(alpha = 0.92f),
                        fontFamily = Alias.body,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(22.dp))
                StatsCard(deck)
                Spacer(Modifier.height(24.dp))
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                if (unlocked) {
                    PillButton(stringResource(R.string.play), onClick = onPlay)
                } else {
                    val label = if (price != null) stringResource(R.string.deck_unlock, price)
                                else stringResource(R.string.deck_unlock_plain)
                    PillButton(label, onClick = onBuy)
                    Spacer(Modifier.height(12.dp))
                    // "Try for free": watch a rewarded ad, then play this session.
                    PillButton(stringResource(R.string.try_for_free), kind = PillKind.Light, onClick = onTryFree)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.restore_purchases),
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = Alias.body,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).clickable { Billing.refresh() },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(deck: BoardGame) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 20.dp),
    ) {
        StatRow(stringResource(R.string.deck_age), deck.age)
        Divider()
        StatRow(stringResource(R.string.deck_players), deck.players)
        Divider()
        StatRow(stringResource(R.string.deck_time), deck.time)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontFamily = Alias.body,
            fontWeight = FontWeight.Black, fontSize = 12.sp)
        Text(value, color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.15f)))
}
