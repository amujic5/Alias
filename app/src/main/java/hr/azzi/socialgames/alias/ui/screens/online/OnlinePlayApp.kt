package hr.azzi.socialgames.alias.ui.screens.online

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.serializedName
import hr.azzi.socialgames.alias.ai.AuthService
import hr.azzi.socialgames.alias.online.*
import hr.azzi.socialgames.alias.ui.screens.ai.SetUsernameScreen
import hr.azzi.socialgames.alias.ui.theme.*
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ helpers */

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun deckDisplayName(context: Context, deck: BoardGame): String {
    val id = context.resources.getIdentifier(deck.name, "string", context.packageName)
    return if (id != 0) context.getString(id) else deck.name
}

fun teamColor(index: Int): Color = Alias.confetti[index % Alias.confetti.size]

/* ------------------------------------------------------------------- router */

@Composable
fun OnlinePlayApp(onExit: () -> Unit) {
    val context = LocalContext.current
    val store: OnlineStore = viewModel()

    val room by store.room.collectAsState()
    val dismissed by store.dismissed.collectAsState()
    val error by store.error.collectAsState()

    var authVersion by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var needsUsername by remember { mutableStateOf(false) }
    var unameBusy by remember { mutableStateOf(false) }
    var unameError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val signInLauncher = rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
        if (res.resultCode == Activity.RESULT_OK) authVersion++
    }
    fun launchSignIn() {
        val providers = listOf(AuthUI.IdpConfig.GoogleBuilder().build())
        signInLauncher.launch(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build()
        )
    }

    LaunchedEffect(authVersion) {
        ready = false
        needsUsername = if (AuthService.isSignedIn) AuthService.needsUsername() else false
        ready = true
    }

    // Round-end interstitial (needs an Activity context to show).
    LaunchedEffect(Unit) {
        store.showAd.collect { context.findActivity()?.let { OnlineInterstitial.show(it) } }
    }
    LaunchedEffect(dismissed) { if (dismissed) onExit() }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { store.clearError() },
            confirmButton = { TextButton(onClick = { store.clearError() }) { Text(stringResource(R.string.online_ok)) } },
            title = { Text(stringResource(R.string.online_title)) },
            text = { Text(error ?: "") },
        )
    }

    when {
        !AuthService.isSignedIn -> OnlineSignInScreen(onSignIn = { launchSignIn() })
        !ready -> BrandBackground { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
        needsUsername -> SetUsernameScreen(busy = unameBusy, error = unameError, onSubmit = { handle ->
            scope.launch {
                unameBusy = true; unameError = null
                try {
                    if (!AuthService.isUsernameAvailable(handle)) unameError = "taken"
                    else { AuthService.setUsername(handle); authVersion++ }
                } catch (e: Exception) { unameError = e.message } finally { unameBusy = false }
            }
        })
        else -> {
            val r = room
            when {
                r == null -> OnlineEntryScreen(store, onExit)
                r.status == OnlineStatus.LOBBY -> OnlineLobbyScreen(store, r)
                r.status == OnlineStatus.PLAYING || r.status == OnlineStatus.FINISHED -> OnlineGameScreen(store, r)
                else -> BrandBackground { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
            }
        }
    }
}

/* ---------------------------------------------------------------- sign-in */

@Composable
private fun OnlineSignInScreen(onSignIn: () -> Unit) {
    BrandBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🎮", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            DisplayText(stringResource(R.string.online_signin_header), size = 28)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.online_signin_prompt),
                color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body,
                fontSize = 16.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PillButton(title = stringResource(R.string.ai_sign_in_google), kind = PillKind.Light, fillWidth = false) { onSignIn() }
        }
    }
}

/* ------------------------------------------------------------------ entry */

@Composable
private fun OnlineEntryScreen(store: OnlineStore, onExit: () -> Unit) {
    var route by remember { mutableStateOf("home") }   // home / create / join
    when (route) {
        "create" -> OnlineCreateScreen(store, onBack = { route = "home" })
        "join" -> OnlineJoinScreen(store, onBack = { route = "home" })
        else -> BrandBackground {
            BackHandler { onExit() }
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
                TopBar(title = stringResource(R.string.online_title), onBack = onExit)
                Spacer(Modifier.height(8.dp))
                Overline(text = stringResource(R.string.online_overline))
                Spacer(Modifier.height(6.dp))
                DisplayText(stringResource(R.string.online_entry_title), size = 32)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.online_entry_subtitle), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(28.dp))
                EntryCard("🎮", Alias.blue600, stringResource(R.string.online_create_title), stringResource(R.string.online_create_sub)) { route = "create" }
                Spacer(Modifier.height(14.dp))
                EntryCard("🔑", Alias.accent, stringResource(R.string.online_join_title), stringResource(R.string.online_join_sub)) { route = "join" }
            }
        }
    }
}

@Composable
private fun EntryCard(emoji: String, tint: Color, title: String, sub: String, onClick: () -> Unit) {
    CCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 22.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(2.dp))
                Text(sub, color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
            }
            Text("›", color = Alias.textSecondary.copy(alpha = 0.6f), fontSize = 28.sp)
        }
    }
}

/* ------------------------------------------------------------------ create */

@Composable
private fun OnlineCreateScreen(store: OnlineStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val busy by store.busy.collectAsState()
    val decks = remember { OnlineWordSource.decks }
    var deck by remember { mutableStateOf(decks.firstOrNull()) }
    val languages = remember(deck) { deck?.let { OnlineWordSource.languages(context, it) } ?: emptyList() }
    var lang by remember(deck) { mutableStateOf(languages.firstOrNull()) }
    var players by remember { mutableIntStateOf(4) }
    var goal by remember { mutableIntStateOf(40) }
    var seconds by remember { mutableIntStateOf(60) }
    var showDeckPicker by remember { mutableStateOf(false) }
    var showLangPicker by remember { mutableStateOf(false) }

    if (showDeckPicker) {
        OnlinePickerScreen(
            title = stringResource(R.string.online_choose_dictionary),
            items = decks,
            label = { deckDisplayName(context, it) },
            imageName = { it.image },
            onPick = { deck = it; showDeckPicker = false },
            onBack = { showDeckPicker = false },
        )
        return
    }
    if (showLangPicker) {
        OnlinePickerScreen(
            title = stringResource(R.string.online_choose_language),
            items = languages,
            label = { it.second },
            emoji = { OnlineWordSource.flag(it.first) },
            onPick = { lang = it; showLangPicker = false },
            onBack = { showLangPicker = false },
        )
        return
    }

    BackHandler { onBack() }
    Box(Modifier.fillMaxSize()) {
        BrandBackground {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp).verticalScroll(rememberScrollState())) {
                TopBar(title = stringResource(R.string.online_create_title), onBack = onBack)
                Spacer(Modifier.height(16.dp))
                // ROOM
                CCard(Modifier.fillMaxWidth()) {
                    Column {
                        Overline(stringResource(R.string.online_section_room), color = Alias.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        SegRow(stringResource(R.string.online_players), listOf(4, 6, 8), players) { players = it }
                        Text(stringResource(R.string.online_players_helper, players, players / 2),
                            color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                // GAME
                CCard(Modifier.fillMaxWidth()) {
                    Column {
                        Overline(stringResource(R.string.online_section_game), color = Alias.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        MenuRow(stringResource(R.string.online_dictionary), deck?.let { deckDisplayName(context, it) } ?: "—") { showDeckPicker = true }
                        Divider(color = Alias.divider)
                        MenuRow(stringResource(R.string.online_language), lang?.let { "${OnlineWordSource.flag(it.first)}  ${it.second}" } ?: "—") { showLangPicker = true }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // MATCH
                CCard(Modifier.fillMaxWidth()) {
                    Column {
                        Overline(stringResource(R.string.online_section_match), color = Alias.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        SegRow(stringResource(R.string.online_score), listOf(40, 60, 80), goal) { goal = it }
                        SegRow(stringResource(R.string.online_round_time), listOf(30, 60, 90), seconds) { seconds = it }
                    }
                }
                Spacer(Modifier.height(24.dp))
                PillButton(stringResource(R.string.online_create_room_btn), enabled = deck != null && lang != null && !busy) {
                    val d = deck; val l = lang
                    if (d != null && l != null) store.createRoom(d, l.first, l.second, players, goal, seconds)
                }
            }
        }
        if (busy) LoadingOverlay(stringResource(R.string.online_creating))
    }
}

/* ------------------------------------------------------------------- join */

@Composable
private fun OnlineJoinScreen(store: OnlineStore, onBack: () -> Unit) {
    val busy by store.busy.collectAsState()
    val myRooms by store.myRooms.collectAsState()
    val clipboard = LocalClipboardManager.current
    var code by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { store.loadMyRooms() }

    BackHandler { onBack() }
    Box(Modifier.fillMaxSize()) {
        BrandBackground {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
                TopBar(title = stringResource(R.string.online_join_title), onBack = onBack)
                Spacer(Modifier.height(20.dp))
                DisplayText(stringResource(R.string.online_enter_code), size = 32)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.online_enter_code_sub), color = Color.White.copy(alpha = 0.85f), fontFamily = Alias.body, fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(20.dp))
                CodeBoxes(code) { code = it }
                Spacer(Modifier.height(16.dp))
                PillButton(stringResource(R.string.online_paste_invite), kind = PillKind.Light) {
                    val raw = clipboard.getText()?.text.orEmpty()
                    code = raw.trim().substringAfterLast('/').uppercase().filter { it.isLetterOrDigit() }.take(6)
                }
                if (myRooms.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Overline(stringResource(R.string.online_recent_rooms))
                    Spacer(Modifier.height(10.dp))
                    myRooms.forEach { r ->
                        RecentRoomCard(r) { store.joinRoom(r.code) }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                PillButton(stringResource(R.string.online_join_room_btn), enabled = code.length >= 4 && !busy) {
                    store.joinRoom(code)
                }
            }
        }
        if (busy) LoadingOverlay(stringResource(R.string.online_joining))
    }
}

@Composable
private fun CodeBoxes(code: String, count: Int = 6, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    Box {
        BasicTextField(
            value = code,
            onValueChange = { onChange(it.uppercase().filter { c -> c.isLetterOrDigit() }.take(count)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            modifier = Modifier.focusRequester(focus).size(1.dp).alpha(0f),
        )
        Row(
            Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.requestFocus() },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(count) { i ->
                val ch = code.getOrNull(i)?.toString() ?: ""
                val focused = i == code.length.coerceAtMost(count - 1) && code.length <= i
                Box(
                    Modifier.weight(1f).aspectRatio(0.82f).clip(RoundedCornerShape(14.dp)).background(Color.White)
                        .then(if (i == code.length) Modifier.border(2.5.dp, Alias.accent, RoundedCornerShape(14.dp)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) { Text(ch, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp) }
            }
        }
    }
}

@Composable
private fun RecentRoomCard(room: OnlineRoom, onRejoin: () -> Unit) {
    CCard(Modifier.fillMaxWidth().clickable { onRejoin() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(room.deckName.ifEmpty { stringResource(R.string.online_title) }, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Spacer(Modifier.height(2.dp))
                Text("Code · ${room.code}", color = Alias.textSecondary, fontFamily = Alias.body, fontSize = 13.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.blue600.copy(alpha = 0.12f)).padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text(stringResource(R.string.online_rejoin), color = Alias.blue600, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
        }
    }
}

/* --------------------------------------------------------- shared widgets */

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // 44dp touch target, nudged left so the chevron sits flush with the content edge.
        Box(
            Modifier.offset(x = (-10).dp).size(44.dp).clip(CircleShape).clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(title.uppercase(), color = Color.White, fontFamily = Alias.display, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.2.sp)
        if (trailing != null) { Spacer(Modifier.weight(1f)); trailing() }
    }
}

@Composable
private fun MenuRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Alias.textPrimary, fontFamily = Alias.body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text("  ›", color = Alias.textSecondary.copy(alpha = 0.6f), fontSize = 20.sp)
    }
}

@Composable
private fun <T> SegRow(label: String, options: List<T>, selection: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        Spacer(Modifier.weight(1f))
        Segmented(options, selection, onSelect)
    }
}

@Composable
private fun <T> Segmented(options: List<T>, selection: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50)).background(Alias.fieldBg).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { opt ->
            val on = opt == selection
            Box(
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .background(if (on) Alias.accent else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 15.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(opt.toString(), color = if (on) Alias.onAccent else Alias.textSecondary,
                    fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun <T> OnlinePickerScreen(
    title: String,
    items: List<T>,
    label: (T) -> String,
    onPick: (T) -> Unit,
    onBack: () -> Unit,
    imageName: ((T) -> String?)? = null,
    emoji: ((T) -> String)? = null,
) {
    val ctx = LocalContext.current
    BackHandler { onBack() }
    BrandBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
            TopBar(title = title, onBack = onBack)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(items) { item ->
                    val imgId = imageName?.invoke(item)?.let { ctx.resources.getIdentifier(it, "drawable", ctx.packageName) } ?: 0
                    Column(
                        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
                            .clickable { onPick(item) }.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (imgId != 0) {
                            Image(painterResource(imgId), null, Modifier.size(64.dp), contentScale = ContentScale.Fit)
                        } else if (emoji != null) {
                            Text(emoji(item), fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(label(item), color = Alias.textPrimary, fontFamily = Alias.display, fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(text, color = Color.White, fontFamily = Alias.body, fontSize = 15.sp)
        }
    }
}

/* ---- permission helper (used by lobby) ---- */

@Composable
fun rememberPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasAvPermissions(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        granted = res.values.all { it }
    }
    val request = {
        launcher.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO))
    }
    return granted to request
}

fun hasAvPermissions(context: Context): Boolean {
    val cam = context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val mic = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return cam && mic
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
