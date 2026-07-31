package hr.azzi.socialgames.alias.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.mutableStateOf
import java.util.Locale
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.CameraInfoService
import hr.azzi.socialgames.alias.Service.DualCameraSupport
import hr.azzi.socialgames.alias.ui.screens.online.hasAvPermissions
import hr.azzi.socialgames.alias.ui.screens.online.openAppSettings
import hr.azzi.socialgames.alias.ui.theme.Alias
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import hr.azzi.socialgames.alias.ui.theme.DisplayText
import hr.azzi.socialgames.alias.ui.theme.Overline
import hr.azzi.socialgames.alias.ui.theme.PillButton
import hr.azzi.socialgames.alias.ui.theme.PillKind

@Composable
fun SettingsScreen(
    teams: List<Team>,
    dictionaries: List<DictionaryModel>,
    boardGame: BoardGame,
    onBack: () -> Unit,
    onPlay: (dictionaryIndex: Int, time: Int, score: Int, captureMoment: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gameSettings", android.content.Context.MODE_PRIVATE) }
    var time by remember { mutableIntStateOf(60) }    // range 20..80
    var score by remember { mutableIntStateOf(100) }  // range 40..120
    var selected by remember { mutableIntStateOf(preferredDictionaryIndex(dictionaries, prefs.getString("selectedLanguageCode", null))) }
    var showPicker by remember { mutableStateOf(false) }

    // Capture Moment needs two cameras at once, and the word cards in Junior and Taboo
    // leave no room for the camera stage — so those two never offer it.
    val captureMomentAvailable = remember {
        !boardGame.isTaboo && !boardGame.isAliasJunior && DualCameraSupport.isSupported(context)
    }
    var captureMoment by remember { mutableStateOf(false) }
    var showDualCamSheet by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            captureMoment = true
        } else {
            // A plain "no" is taken at face value. Only a permanent denial — the system
            // stops asking, so a second tap would look broken — is worth a detour to the
            // app settings.
            val activity = context.findActivity()
            val permanentlyDenied = activity != null && granted.keys.none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            if (permanentlyDenied) showPermissionDialog = true
        }
    }

    fun toggleCaptureMoment(on: Boolean) {
        if (!on) { captureMoment = false; return }
        if (hasAvPermissions(context)) {
            captureMoment = true
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // Opens itself on the first few visits, then only now and again — see CameraInfoService.
    LaunchedEffect(captureMomentAvailable) {
        if (captureMomentAvailable && !captureMoment && CameraInfoService.shouldShowInfo(context)) {
            showDualCamSheet = true
            CameraInfoService.infoShowed(context)
        }
    }

    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.settings).uppercase(), 22)
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (teams.isNotEmpty()) VsPill(teams.filter { it.playing }.map { it.teamName })

                SettingSlider(stringResource(R.string.score_settings).uppercase(), score, 40, 120) { score = it }
                SettingSlider(stringResource(R.string.time_limit_seconds).uppercase(), time, 20, 80) { time = it }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Overline("DICTIONARY")
                    val dict = dictionaries.getOrNull(selected)
                    Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f))
                        .clickable { showPicker = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(flagEmoji(dict?.imageURLString), fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(dict?.language ?: "", color = Color.White, fontFamily = Alias.body, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                }

                if (captureMomentAvailable) {
                    CaptureMomentRow(
                        checked = captureMoment,
                        onCheckedChange = { toggleCaptureMoment(it) },
                        onInfo = { showDualCamSheet = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                PillButton(stringResource(R.string.play), PillKind.Primary) {
                    onPlay(selected, time, score, captureMoment && captureMomentAvailable)
                }
            }
        }
    }

    if (showDualCamSheet) {
        DualCameraSheet(
            isOn = captureMoment,
            onToggle = { toggleCaptureMoment(it) },
            onDismiss = { showDualCamSheet = false },
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.camera_permission_title), fontFamily = Alias.body, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.camera_permission_message), fontFamily = Alias.body) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    openAppSettings(context)
                }) { Text(stringResource(R.string.settings), fontFamily = Alias.body, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel), fontFamily = Alias.body)
                }
            },
        )
    }

    if (showPicker) {
        LanguagePickerOverlay(
            dictionaries = dictionaries,
            selectedIndex = selected,
            onDismiss = { showPicker = false },
            onPick = { idx ->
                selected = idx
                dictionaries.getOrNull(idx)?.languageCode?.let { prefs.edit().putString("selectedLanguageCode", it).apply() }
                showPicker = false
            },
        )
    }
}

/** The hosting Activity — needed for `shouldShowRequestPermissionRationale`, and reached
 *  by unwrapping because a Compose context can be a themed wrapper rather than the
 *  Activity itself. */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * The CAPTURE MOMENT row: overline title, an ⓘ that reopens the explainer, a one-line
 * subtitle, and the Figma checkbox (26dp, 2dp accent border). Only ever shown on devices
 * that can actually film both cameras at once.
 */


@Composable
private fun VsPill(names: List<String>) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(names.joinToString("   VS   "), color = Color.White, fontFamily = Alias.display,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun SettingSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Overline(label)
            DisplayText("$value", 24, color = Alias.accent)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Alias.accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$min", color = Color.White.copy(alpha = 0.7f), fontFamily = Alias.body, fontSize = 12.sp)
            Text("$max", color = Color.White.copy(alpha = 0.7f), fontFamily = Alias.body, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LanguagePickerOverlay(
    dictionaries: List<DictionaryModel>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(false) {}) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                DisplayText(stringResource(R.string.ai_choose_language), 22)
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(dictionaries) { i, d ->
                    LanguageCard(flagEmoji(d.imageURLString), d.language, i == selectedIndex) { onPick(i) }
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(emoji: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().height(120.dp).clip(shape).background(Color.White)
            .border(if (selected) 3.dp else 0.dp, if (selected) Alias.accent else Color.Transparent, shape)
            .clickable { onClick() }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
        Text(name, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Emoji flag for a dictionary's `imageURLString` (drawable name) — text icon, no image asset. */
private fun flagEmoji(name: String?): String = when (name) {
    "uk" -> "🇬🇧"; "croatia" -> "🇭🇷"; "france" -> "🇫🇷"; "serbia" -> "🇷🇸"
    "germany" -> "🇩🇪"; "russia" -> "🇷🇺"; "dutch" -> "🇳🇱"; "italy" -> "🇮🇹"
    "bosnia" -> "🇧🇦"; "slovenia" -> "🇸🇮"; "sweden" -> "🇸🇪"; "spain" -> "🇪🇸"
    else -> "🏳️"
}

/** Best-guess dictionary languageCode from the device language, then region. */
private fun deviceDictionaryCode(): String? {
    when (Locale.getDefault().language.lowercase()) {
        "en" -> return "ENG"; "hr" -> return "CRO"; "nl" -> return "DUT"; "es" -> return "ESP"
        "fr" -> return "FRA"; "de" -> return "GER"; "it" -> return "ITA"; "ru" -> return "RUS"
        "sl" -> return "SLO"; "sr" -> return "SRB"; "sv" -> return "SWE"; "bs" -> return "BOS"
    }
    return when (Locale.getDefault().country.uppercase()) {
        "GB", "US", "IE", "AU", "CA", "NZ" -> "ENG"
        "HR" -> "CRO"; "NL", "BE" -> "DUT"; "ES" -> "ESP"; "FR", "LU", "MC" -> "FRA"
        "DE", "AT", "CH", "LI" -> "GER"; "IT", "SM", "VA" -> "ITA"; "RU" -> "RUS"
        "SI" -> "SLO"; "RS", "ME" -> "SRB"; "SE" -> "SWE"; "BA" -> "BOS"
        else -> null
    }
}

/** Preselect: 1) the in-app UI language (LanguageManager), 2) saved pref,
 *  3) device locale/region, 4) first dictionary. The deck default always
 *  follows the app language, mirroring iOS. */
private fun preferredDictionaryIndex(dictionaries: List<DictionaryModel>, savedCode: String?): Int {
    if (dictionaries.isEmpty()) return 0
    fun idx(code: String) = dictionaries.indexOfFirst { it.languageCode.equals(code, ignoreCase = true) }
    idx(hr.azzi.socialgames.alias.Service.LanguageManager.deckLanguageCode).takeIf { it >= 0 }?.let { return it }
    savedCode?.let { idx(it) }?.takeIf { it >= 0 }?.let { return it }
    deviceDictionaryCode()?.let { idx(it) }?.takeIf { it >= 0 }?.let { return it }
    return 0
}
