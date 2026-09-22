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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.arena.AliasAvatar
import hr.azzi.socialgames.alias.arena.ArenaAnalytics
import hr.azzi.socialgames.alias.arena.ArenaPalette
import hr.azzi.socialgames.alias.ui.theme.Alias

/**
 * The one-time announcement for Alias Arena.
 *
 * Android port of iOS `ArenaPromoView.swift`.
 *
 * A promo, not a screen: it has one job, it is dismissible three ways, and it is drawn
 * over whatever the player was already looking at. When it is shown is somebody else's
 * decision — this takes a count, an enter and a dismiss and knows nothing about when or
 * how often.
 *
 * **Fixed dark, deliberately.** Everything else follows the Light / Dark / System switch
 * and this does not: the navy gradient, the gold ring and the pink badge are one branded
 * surface drawn as a unit, and repainting half of it for daylight would leave gold on
 * white.
 */
@Composable
fun ArenaPromoSheet(
    playersOnline: Int? = null,
    onEnter: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { ArenaAnalytics.promoShown() }

    // Every way out counts, not only the button: "Maybe later", the ×, and a tap outside
    // are one answer given three ways, and splitting them would make the funnel not add up.
    val dismiss = { ArenaAnalytics.promoDismissed(); onDismiss() }
    val enter = { ArenaAnalytics.promoEntered(); onEnter() }

    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = dismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 14.dp, vertical = 20.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF133B6E), Color(0xFF091B33))))
                    .border(1.dp, Color(0xFF5EB8FA).copy(alpha = 0.42f), RoundedCornerShape(28.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(dismiss)
                Mascot(playersOnline)
                Spacer(Modifier.height(16.dp))
                Benefits()
                Spacer(Modifier.height(16.dp))
                Actions(enter, dismiss)
            }
        }
    }
}

@Composable
private fun Header(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
        Text(
            stringResource(R.string.arena_promo_badge),
            fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
            letterSpacing = 0.66.sp, color = Color.White, maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter)
                .clip(RoundedCornerShape(50)).background(ArenaPalette.live)
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
        Box(
            Modifier.align(Alignment.TopEnd).size(38.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }

    Text(
        stringResource(R.string.arena_entry_title).uppercase(),
        fontFamily = Alias.display, fontSize = 40.sp, color = Color.White,
        maxLines = 1, textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    Text(
        stringResource(R.string.arena_promo_subtitle),
        fontFamily = Alias.body, fontWeight = FontWeight.Medium, fontSize = 14.sp,
        color = Color(0xFFCFE8FF), textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp),
    )
}

/** The bot in its gold ring, with the live count hung off the bottom. */
@Composable
private fun Mascot(playersOnline: Int?) {
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.padding(top = 14.dp)) {
        Box(Modifier.padding(bottom = 14.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(154.dp).clip(CircleShape)
                    .border(5.dp, Color(0xFFFFD23D), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ArenaAvatarImage(AliasAvatar.DEFAULT.id, 134.dp, ring = Color(0xFFFFD23D), ringWidth = 4.dp)
            }
        }

        // Hidden when the count is not known — a live count is a reason to come in, and
        // "0 PLAYING" is a reason not to.
        if (playersOnline != null && playersOnline > 0) {
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFF0E1726).copy(alpha = 0.94f))
                    .border(1.dp, Color(0xFFFFD23D).copy(alpha = 0.65f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF33C759)))
                Text(
                    stringResource(R.string.arena_promo_playing, playersOnline),
                    fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
                    color = Color.White, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Benefits() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Text(
            stringResource(R.string.arena_promo_whats_inside),
            fontFamily = Alias.body, fontWeight = FontWeight.Black, fontSize = 11.sp,
            letterSpacing = 1.1.sp, color = Color(0xFFCFE8FF),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Benefit("01", Color(0xFFFFD23D),
                stringResource(R.string.arena_promo_b1_title),
                stringResource(R.string.arena_promo_b1_sub), Modifier.weight(1f))
            Benefit("#", ArenaPalette.live,
                stringResource(R.string.arena_promo_b2_title),
                stringResource(R.string.arena_promo_b2_sub), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Benefit(glyph: String, tint: Color, title: String, detail: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF12365E).copy(alpha = 0.92f))
            .border(1.dp, Color(0xFF5EB8FA).copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, fontFamily = Alias.display, fontSize = 16.sp, color = Color(0xFF0E1726))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontFamily = Alias.display, fontSize = 16.sp, color = Color.White, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Text(detail, fontFamily = Alias.body, fontWeight = FontWeight.Medium, fontSize = 11.sp,
            color = Color(0xFFCFE8FF), maxLines = 3)
    }
}

@Composable
private fun Actions(onEnter: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFD23D))
                .clickable(onClick = onEnter)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.arena_promo_cta),
                fontFamily = Alias.display, fontSize = 20.sp, color = Color(0xFF0E1726),
                maxLines = 1, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF0E1726), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.arena_promo_later),
            fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = Color(0xFFCFE8FF),
            modifier = Modifier.clickable(onClick = onDismiss).padding(6.dp),
        )
    }
}
