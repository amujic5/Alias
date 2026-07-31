package hr.azzi.socialgames.alias.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.azzi.socialgames.alias.BuildConfig
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.GameSettings
import hr.azzi.socialgames.alias.Service.AppLanguage
import hr.azzi.socialgames.alias.Service.LanguageManager
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import kotlinx.datetime.format.Padding

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onHowToPlay: () -> Unit,
    onShare: () -> Unit,
    onAiChallenge: () -> Unit,
    onOnlinePlay: () -> Unit,
) {
    val context = LocalContext.current
    // Subscribe to configuration so the whole screen recomposes when the UI language
    // is switched in place (HomeActivity handles the locale configChange instead of
    // being relaunched), refreshing the flag image and every LanguageManager-derived value.
    LocalConfiguration.current
    var showLanguages by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(GameSettings.soundEnabled(context)) }
    var vibrateEnabled by remember { mutableStateOf(GameSettings.vibrateEnabled(context)) }

    BrandBackground {
        // Country background image for the selected language (bottom-aligned, subtle).
        val bgName = LanguageManager.homeImageName
        if (bgName.isNotEmpty()) {
            val resId = remember(bgName) { context.resources.getIdentifier(bgName, "drawable", context.packageName) }
            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.BottomCenter,
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 30.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Upper half — gear, logo and "Play online". Given equal weight to the lower
            // half (below), the 10dp gaps + "Play with AI" between them put that button on
            // the exact vertical centre of the screen, regardless of screen size.
            Column(
                Modifier.weight(0.8f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top bar: settings (language) gear on the right — frosted circle to match
                // the Figma home (same treatment as the menu-button badges / iOS gear).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        Modifier.size(50.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ScatteredSquares()
                    HomeTitle()
                }
                Spacer(Modifier.weight(1f))
            }


            Column(
                Modifier.weight(1.2f).fillMaxWidth().padding(horizontal = 34.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {

                MenuButton(
                    text = stringResource(R.string.play_online),
                    textColor = Color.White,
                    background = SolidColor(Color(0xFF2685F5)),
                    onClick = onOnlinePlay,
                ) { Badge(Color.White.copy(alpha = 0.22f)) { BadgeIcon(Icons.Filled.Public) } }

                // Play with AI — pinned to the vertical centre (pink→orange gradient).
                MenuButton(
                    text = stringResource(R.string.play_with_ai),
                    textColor = Color.White,
                    background = Brush.horizontalGradient(listOf(Color(0xFFF54A87), Color(0xFFFA8538))),
                    onClick = onAiChallenge,
                ) { Badge(Color.White.copy(alpha = 0.22f)) { BadgeIcon(Icons.Filled.SmartToy) } }
                // New game — solid yellow, dark label, darker-orange badge.
                MenuButton(
                    text = stringResource(R.string.new_game),
                    textColor = Color(0xFF0E1726),
                    background = SolidColor(Color(0xFFFFC91F)),
                    onClick = onNewGame,
                ) { Badge(Color(0xFFF58C0D)) { BadgeIcon(Icons.Filled.Star) } }

                // How to play — translucent black, narrower, white outline.
                MenuButton(
                    text = stringResource(R.string.how_to_play),
                    textColor = Color.White,
                    background = SolidColor(Color.Black.copy(alpha = 0.5f)),
                    onClick = onHowToPlay,
                    modifier = Modifier.fillMaxWidth(0.88f),
                    height = 64.dp,
                ) {
                    Badge(Color.White.copy(alpha = 0.16f), border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))) {
                        BadgeIcon(Icons.AutoMirrored.Filled.HelpOutline)
                    }
                }
            }
        }
    }

    // The gear opens settings, and the language picker is one row inside it rather than the
    // whole of what the gear does.
    if (showSettings) {
        AppSettingsScreen(
            language = LanguageManager.current.nativeName,
            soundEnabled = soundEnabled,
            vibrateEnabled = vibrateEnabled,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            onBack = { showSettings = false },
            onLanguage = { showLanguages = true },
            onSoundChanged = { soundEnabled = it; GameSettings.setSoundEnabled(context, it) },
            onVibrateChanged = { vibrateEnabled = it; GameSettings.setVibrateEnabled(context, it) },
            onMedia = { showHistory = true },
            onPrivacy = { openUrl(context, PRIVACY_URL) },
            onTerms = { openUrl(context, TERMS_URL) },
            onShare = onShare,
        )
    }

    if (showHistory) {
        RoundHistoryRoute(onBack = { showHistory = false })
    }

    if (showLanguages) {
        LanguagePickerScreen(
            current = LanguageManager.currentCode,
            onDismiss = { showLanguages = false },
            onPick = { code ->
                showLanguages = false
                // Applies the locale app-wide and recreates the UI.
                LanguageManager.setLanguage(code)
            },
        )
    }
}

/** The app's own pages. Confirm these before release — they are the site's expected paths. */
private const val PRIVACY_URL = "https://aliaswords.com/privacy"
private const val TERMS_URL = "https://aliaswords.com/terms"

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)),
        )
    }
}

/** App UI-language chooser (frosted list of flag + native name). */
@Composable
private fun LanguagePickerScreen(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                DisplayText(stringResource(R.string.settings_language), 24)
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(LanguageManager.available) { lang ->
                    LanguageRow(lang, lang.code == current) { onPick(lang.code) }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(lang: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = if (selected) 0.16f else 0.08f))
            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(lang.flag, fontSize = 24.sp)
        Spacer(Modifier.size(14.dp))
        Text(lang.nativeName, color = Color.White, fontFamily = Alias.display,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = Alias.accent)
    }
}

/** A single menu-card pill: 68dp tall, white 2dp outline, centered label with a leading
 *  round badge. Height and width can be overridden (How-to-play is shorter and narrower). */
@Composable
private fun MenuButton(
    text: String,
    textColor: Color,
    background: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Dp = 74.dp,
    border: BorderStroke = BorderStroke(3.dp, Color.White),
    badge: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier.height(height).clip(shape)
            .background(background)
            .border(border, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart).padding(start = 9.dp)) { badge() }
        Text(
            text, color = textColor, fontFamily = Alias.display,
            fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
        )
    }
}

/** The round 44dp icon badge on the left of a [MenuButton]. */
@Composable
private fun Badge(background: Color, border: BorderStroke? = null, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(background)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun BadgeIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
}

/** Home logo "ALIAS / WORDS" — Baloo 2 ExtraBold, 64sp, tight leading, with the hard
 *  navy drop-shadow from the Figma home (0px 6px 0px rgba(10,46,89,0.35)). */
@Composable
private fun HomeTitle() {
    val shadow = Shadow(
        color = Color(0xFF0A2E59).copy(alpha = 0.35f),
        offset = with(LocalDensity.current) { Offset(0f, 6.dp.toPx()) },
        blurRadius = 0f,
    )
    Text(
        "ALIAS\nWORDS",
        color = Color.White,
        fontFamily = Alias.display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 78.sp,
        lineHeight = 78.sp,
        textAlign = TextAlign.Center,
        style = TextStyle(
            shadow = shadow,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}

/** Decorative floating rounded squares behind the logo (matches Figma home). */
@Composable
private fun ScatteredSquares() {
    val items = listOf(
        Triple(Alias.confetti[0], (-120).dp to (-90).dp, 44),
        Triple(Alias.confetti[1], (120).dp to (-70).dp, 36),
        Triple(Alias.confetti[2], (-130).dp to (60).dp, 40),
        Triple(Alias.confetti[3], (110).dp to (90).dp, 30),
        Triple(Alias.confetti[4], (-70).dp to (120).dp, 26),
        Triple(Alias.confetti[5], (80).dp to (-130).dp, 32),
    )
    items.forEachIndexed { i, (color, off, sz) ->
        Box(
            Modifier
                .offset(x = off.first, y = off.second)
                .size(sz.dp)
                .rotate((i * 17 % 30 - 15).toFloat())
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
    }
}
