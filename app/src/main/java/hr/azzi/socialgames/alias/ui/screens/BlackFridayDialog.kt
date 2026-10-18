package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.BlackFriday
import hr.azzi.socialgames.alias.ui.theme.Alias
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private object BF {
    val dim = Color(0xFF083F75)
    val cardTop = Color(0xFF0AA8FF)
    val cardMid = Color(0xFF067AFF)
    val cardBottom = Color(0xFF0552E0)
    val ink = Color(0xFF0E1726)
    val gold = Color(0xFFFFD23D)
    val blue600 = Color(0xFF0F70D1)
    val blue300 = Color(0xFF5FB7FB)
    val blue200 = Color(0xFF9FD4FF)
    val blue100 = Color(0xFFCFE9FF)
    val buyTop = Color(0xFFFFE54D)
    val buyBottom = Color(0xFFFFBD0A)
}

/**
 * The Black Friday offer, in either of its two phases.
 *
 * Android port of iOS `BlackFridayPromoView.swift` — Figma "Black Friday · 01 Coming soon"
 * (4054:738) and "· 02 Live" (4054:848).
 *
 * One card for both. Before Black Friday it counts down to the opening and its button is a
 * disabled "Available soon"; from Black Friday on it counts down to the close and the
 * button buys. When it is shown is [BlackFriday]'s decision; this only draws what it is
 * given.
 *
 * Fixed colours, like the Arena promo: one branded surface drawn as a unit.
 *
 * @param pricing `null` while Play has not priced everything — the price block is then left
 *   out rather than drawn with holes in it.
 * @param bundlePrice the bundle's own price for the button, which can be known before the
 *   rest of the catalogue is.
 */
@Composable
fun BlackFridayDialog(
    phase: BlackFriday.Phase,
    pricing: BlackFriday.Pricing?,
    bundlePrice: String?,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isSale = phase is BlackFriday.Phase.Sale
    val target = when (phase) {
        is BlackFriday.Phase.Teaser -> phase.opensAt
        is BlackFriday.Phase.Sale -> phase.endsAt
        BlackFriday.Phase.None -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize()
                // Home stays visible under a navy wash; a tap on it is a way out.
                .background(BF.dim.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Card(isSale, target, pricing, bundlePrice, onBuy, onDismiss)
            }
        }
    }
}

@Composable
private fun Card(
    isSale: Boolean,
    target: Instant?,
    pricing: BlackFriday.Pricing?,
    bundlePrice: String?,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.widthIn(max = 350.dp)
            .shadow(14.dp, shape, ambientColor = Color(0xFF001F66), spotColor = Color(0xFF001F66))
            .clip(shape)
            .background(Brush.verticalGradient(0f to BF.cardTop, 0.48f to BF.cardMid, 1f to BF.cardBottom))
            .border(2.dp, Color.White, shape)
            // Taps on the card are not taps on the backdrop.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlackFridayChip(width = 276.dp, height = 42.dp, fontSize = 27)
            Title()
            Image(
                painterResource(R.drawable.bf_gift_bundle),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(286.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(R.string.bf_includes), color = Color.White,
                fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            if (pricing != null) PriceBlock(pricing)
            if (target != null) Countdown(isSale, target)
            if (isSale) {
                BuyButton(
                    bundlePrice?.let { stringResource(R.string.bf_unlock_for, it) }
                        ?: stringResource(R.string.bf_unlock_everything),
                    onBuy,
                )
            } else {
                SoonButton()
            }
        }
        val closeLabel = stringResource(R.string.bf_close)
        Text(
            "×", color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                .size(44.dp).clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onDismiss)
                .padding(top = 4.dp)
                .semantics { contentDescription = closeLabel },
        )
    }
}

/**
 * The BLACK FRIDAY chip — ink, gold ring, gold type. The card's header, and on Home the
 * whole offer folded down.
 */
@Composable
fun BlackFridayChip(
    width: Dp? = null,
    height: Dp,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(if (height > 44.dp) 10.dp else 12.dp)
    Box(
        modifier.then(if (width != null) Modifier.width(width) else Modifier)
            .height(height).clip(shape).background(BF.ink).border(2.dp, BF.gold, shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "BLACK FRIDAY", color = BF.gold, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = fontSize.sp, maxLines = 1,
            style = TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
        )
    }
}

@Composable
private fun Title() {
    val shadow = Shadow(
        color = Color(0xFF002E80).copy(alpha = 0.4f),
        offset = with(LocalDensity.current) { Offset(0f, 3.dp.toPx()) },
        blurRadius = 0f,
    )
    Text(
        stringResource(R.string.bf_unlock_all), color = Color.White, fontFamily = Alias.display,
        fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 35.sp,
        textAlign = TextAlign.Center, style = TextStyle(shadow = shadow),
    )
}

/** The full price struck through over the Black Friday price and the discount. */
@Composable
private fun PriceBlock(pricing: BlackFriday.Pricing) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(BF.blue600).border(1.dp, BF.blue300, shape)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.bf_total_value), color = BF.blue100, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
            )
            Text(
                pricing.fullPrice, color = BF.blue100, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                pricing.price, color = Color.White, fontFamily = Alias.display,
                fontWeight = FontWeight.ExtraBold, fontSize = 46.sp, maxLines = 1,
                style = TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
            )
            Text(
                "-${pricing.discountPercent}%", color = BF.ink, fontFamily = Alias.body,
                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center,
                modifier = Modifier.width(72.dp).clip(RoundedCornerShape(12.dp)).background(BF.gold)
                    .padding(vertical = 7.dp),
            )
        }
    }
}

/** Days, hours and minutes to the opening or the close, ticking once a minute. */
@Composable
private fun Countdown(isSale: Boolean, target: Instant) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(target) {
        while (true) {
            now = Instant.now()
            delay(1_000L * (60 - now.epochSecond % 60))
        }
    }
    val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            stringResource(if (isSale) R.string.bf_ends_in else R.string.bf_starts_in),
            color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CountdownCell(minutes / 1440, stringResource(R.string.bf_days))
            CountdownCell(minutes % 1440 / 60, stringResource(R.string.bf_hours))
            CountdownCell(minutes % 60, stringResource(R.string.bf_minutes))
        }
    }
}

@Composable
private fun CountdownCell(value: Long, unit: String) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.size(width = 66.dp, height = 64.dp).clip(shape).background(BF.blue600)
            .border(1.5.dp, Color.White, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "%02d".format(value), color = Color.White, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 26.sp,
        )
        Text(
            unit.uppercase(), color = Color.White, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1,
        )
    }
}

@Composable
private fun BuyButton(title: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(56.dp)) {
        // The hard 4dp drop under the button in the design, drawn as a second pill.
        Box(
            Modifier.fillMaxWidth().height(52.dp).offset(y = 4.dp)
                .background(Color(0xFFA65900).copy(alpha = 0.45f), shape),
        )
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(shape)
                .background(Brush.verticalGradient(listOf(BF.buyTop, BF.buyBottom)))
                .border(BorderStroke(1.5.dp, Color.White), shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title, color = BF.ink, fontFamily = Alias.body, fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp, maxLines = 1,
            )
        }
    }
}

@Composable
private fun SoonButton() {
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.padding(top = 4.dp).fillMaxWidth().height(52.dp).clip(shape).background(BF.blue200)
            .border(1.5.dp, Color.White, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.bf_available_soon), color = Color.White, fontFamily = Alias.body,
            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
        )
    }
}
