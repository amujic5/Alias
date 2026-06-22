package hr.azzi.socialgames.alias.ui.screens.ai

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.ai.AIAnalytics
import hr.azzi.socialgames.alias.ai.AIChallenge
import hr.azzi.socialgames.alias.ai.AIChallengePlay
import hr.azzi.socialgames.alias.ai.AIChallengeRepository
import hr.azzi.socialgames.alias.ai.AIDeckCatalog
import hr.azzi.socialgames.alias.ai.AIPracticeConfig
import hr.azzi.socialgames.alias.ai.AIPracticeResult
import hr.azzi.socialgames.alias.ai.AIUserStats
import hr.azzi.socialgames.alias.ai.AuthService
import hr.azzi.socialgames.alias.ui.theme.BrandBackground
import kotlinx.coroutines.launch

private sealed interface AiRoute {
    object Hub : AiRoute
    data class Setup(val forChallenge: Boolean) : AiRoute
    data class Play(val config: AIPracticeConfig, val fixedWords: List<String>?, val challenge: AIChallenge?, val isChallenge: Boolean) : AiRoute
    data class Rank(val challengeId: String) : AiRoute
    data class Intro(val challenge: AIChallenge) : AiRoute
    data class Outcome(val challenge: AIChallenge, val score: Int, val total: Int) : AiRoute
    data class OpenById(val id: String) : AiRoute
    data class PlayDetail(val play: AIChallengePlay, val challenge: AIChallenge) : AiRoute
    object Profile : AiRoute
}

private sealed interface Pending {
    data class Open(val id: String) : Pending
    object CreateChallenge : Pending
}

private fun deckNameOf(context: Context, deck: hr.azzi.socialgames.alias.ai.AIDeck): String {
    val id = context.resources.getIdentifier(deck.nameRes, "string", context.packageName)
    return if (id != 0) context.getString(id) else deck.id
}

@Composable
fun AiChallengeApp(initialChallengeId: String?, onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stack: SnapshotStateList<AiRoute> = remember { mutableStateListOf(AiRoute.Hub) }
    fun push(r: AiRoute) { stack.add(r) }
    fun replaceTop(r: AiRoute) { if (stack.isNotEmpty()) stack[stack.lastIndex] = r else stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onExit() }
    fun popToHub() { while (stack.size > 1) stack.removeAt(stack.lastIndex) }

    var authVersion by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var needsUsername by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(AIUserStats()) }
    val recent = remember { mutableStateListOf<AIChallenge>() }
    var pending by remember { mutableStateOf<Pending?>(null) }
    var unameBusy by remember { mutableStateOf(false) }
    var unameError by remember { mutableStateOf<String?>(null) }
    var deepLinkConsumed by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) authVersion++
    }
    fun launchSignIn() {
        val providers = listOf(AuthUI.IdpConfig.GoogleBuilder().build())
        signInLauncher.launch(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build()
        )
    }

    fun openChallenge(ch: AIChallenge) {
        val me = AuthService.uid ?: ""
        if (ch.creatorId == me || ch.hasJoined(me)) push(AiRoute.Rank(ch.id)) else push(AiRoute.Intro(ch))
    }

    LaunchedEffect(authVersion) {
        ready = false
        if (AuthService.isSignedIn) {
            needsUsername = AuthService.needsUsername()
            username = AuthService.currentName()
            stats = AuthService.userStats(AuthService.uid!!)
            recent.clear()
            if (!needsUsername) recent.addAll(AIChallengeRepository.recent(AuthService.uid!!, 5))
        } else {
            needsUsername = false; username = ""; stats = AIUserStats(); recent.clear()
        }
        ready = true
        if (AuthService.isSignedIn && !needsUsername) {
            when (val p = pending) {
                is Pending.Open -> { pending = null; push(AiRoute.OpenById(p.id)) }
                Pending.CreateChallenge -> { pending = null; push(AiRoute.Setup(forChallenge = true)) }
                null -> {}
            }
        }
    }

    LaunchedEffect(ready) {
        if (ready && !deepLinkConsumed && initialChallengeId != null) {
            deepLinkConsumed = true
            if (AuthService.isSignedIn && !needsUsername) push(AiRoute.OpenById(initialChallengeId))
            else { pending = Pending.Open(initialChallengeId); if (!AuthService.isSignedIn) launchSignIn() }
        }
    }

    fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        context.startActivity(Intent.createChooser(send, "Share challenge"))
    }

    if (!ready) {
        BrandBackground { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
        return
    }

    if (AuthService.isSignedIn && needsUsername) {
        SetUsernameScreen(busy = unameBusy, error = unameError) { handle ->
            if (!AuthService.isValidHandle(handle)) { unameError = context.getString(R.string.ai_err_handle); return@SetUsernameScreen }
            unameBusy = true; unameError = null
            scope.launch {
                try {
                    if (!AuthService.isUsernameAvailable(handle)) { unameError = context.getString(R.string.ai_err_taken); unameBusy = false; return@launch }
                    AuthService.setUsername(handle)
                    unameBusy = false; authVersion++
                } catch (e: Exception) {
                    unameBusy = false
                    unameError = if (e.message == "username_taken") context.getString(R.string.ai_err_taken) else context.getString(R.string.ai_err_generic)
                }
            }
        }
        return
    }

    val myUid = AuthService.uid ?: ""

    when (val route = stack.last()) {
        AiRoute.Hub -> AiHubScreen(
            isSignedIn = AuthService.isSignedIn, username = username, uid = myUid, stats = stats, recent = recent,
            onBack = { onExit() },
            onSignIn = { launchSignIn() },
            onPractice = { push(AiRoute.Setup(forChallenge = false)) },
            onChallenge = { if (AuthService.isSignedIn) push(AiRoute.Setup(forChallenge = true)) else { pending = Pending.CreateChallenge; launchSignIn() } },
            onProfile = { push(AiRoute.Profile) },
            onSeeAll = { push(AiRoute.Profile) },
            onOpenChallenge = { openChallenge(it) },
        )

        is AiRoute.Setup -> AiSetupScreen(
            title = if (route.forChallenge) stringResource(R.string.ai_challenge) else stringResource(R.string.ai_practice),
            subtitle = if (route.forChallenge) stringResource(R.string.ai_challenge_warmup) else stringResource(R.string.ai_practice_warmup),
            startLabel = if (route.forChallenge) stringResource(R.string.ai_play_your_round) else stringResource(R.string.ai_start_practice),
            onBack = { pop() },
            onStart = { config ->
                if (route.forChallenge) AIAnalytics.challengeCreateOpen(context, config)
                else AIAnalytics.practiceOpen(context, config)
                push(AiRoute.Play(config, null, null, route.forChallenge))
            },
        )

        is AiRoute.Play -> {
            val finish: ((AIPracticeResult, List<String>) -> Unit)? = if (route.isChallenge) { result, frozen ->
                scope.launch {
                    val uid = AuthService.uid ?: return@launch
                    try {
                        if (route.challenge == null) {
                            val ch = AIChallengeRepository.create(
                                route.config, deckNameOf(context, route.config.deck), frozen, result, uid, username)
                            popToHub(); push(AiRoute.Rank(ch.id)); authVersion++
                        } else {
                            AIChallengeRepository.submit(route.challenge.id, uid, username, false, result)
                            replaceTop(AiRoute.Outcome(route.challenge, result.correct, result.played))
                            authVersion++
                        }
                    } catch (e: Exception) {
                        // Network/Firestore failure: don't strand the player on the
                        // "Done" spinner — surface it and return to the hub.
                        Toast.makeText(context, context.getString(R.string.ai_err_generic), Toast.LENGTH_LONG).show()
                        popToHub()
                    }
                }
                Unit
            } else null
            val onClose: () -> Unit = { if (route.isChallenge) pop() else popToHub() }
            if (route.config.mode == hr.azzi.socialgames.alias.ai.AIMode.YOU_EXPLAIN)
                AiPlayScreen(route.config, route.fixedWords, finish, onClose)
            else
                AiExplainScreen(route.config, route.fixedWords, finish, onClose)
        }

        is AiRoute.OpenById -> {
            LaunchedEffect(route.id) {
                val ch = AIChallengeRepository.load(route.id)
                if (ch == null) pop()
                else { val me = AuthService.uid ?: ""; replaceTop(if (ch.creatorId == me || ch.hasJoined(me)) AiRoute.Rank(ch.id) else AiRoute.Intro(ch)) }
            }
            BrandBackground { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
        }

        is AiRoute.Rank -> {
            var ch by remember(route.challengeId) { mutableStateOf<AIChallenge?>(null) }
            val plays = remember(route.challengeId) { mutableStateListOf<AIChallengePlay>() }
            LaunchedEffect(route.challengeId) {
                ch = AIChallengeRepository.load(route.challengeId)
                plays.clear(); plays.addAll(AIChallengeRepository.plays(route.challengeId))
            }
            val c = ch
            if (c == null) BrandBackground { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
            else AiRankScreen(c, plays, myUid,
                onShare = { AIAnalytics.challengeShareTapped(context, c.id); shareText(context.getString(R.string.ai_share_beat, c.shareUrl)) },
                onOpenPlay = { push(AiRoute.PlayDetail(it, c)) },
                onClose = { pop() })
        }

        is AiRoute.PlayDetail -> AiPlayDetailScreen(route.play, route.challenge, onClose = { pop() })

        is AiRoute.Intro -> AiIntroScreen(
            challenge = route.challenge,
            onPlay = {
                scope.launch {
                    val uid = AuthService.uid
                    // Join is a precondition: if it fails, stay on the intro and let
                    // the player retry rather than entering a round we can't record.
                    val joined = uid != null &&
                        runCatching { AIChallengeRepository.join(route.challenge, uid, username) }.isSuccess
                    if (!joined) {
                        Toast.makeText(context, context.getString(R.string.ai_err_generic), Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val deck = AIDeckCatalog.deck(route.challenge.deckId) ?: AIDeckCatalog.decks.first()
                    val config = AIPracticeConfig(deck, route.challenge.aiLanguage, route.challenge.totalSeconds)
                    replaceTop(AiRoute.Play(config, route.challenge.words, route.challenge, true))
                }
            },
            onClose = { pop() },
        )

        is AiRoute.Outcome -> AiOutcomeScreen(
            challenge = route.challenge, myName = username, myScore = route.score, myTotal = route.total, stats = stats,
            onShare = { AIAnalytics.challengeShareTapped(context, route.challenge.id); shareText(context.getString(R.string.ai_share_scored, route.score, route.challenge.creatorName, route.challenge.shareUrl)) },
            onSeeBoard = { replaceTop(AiRoute.Rank(route.challenge.id)) },
            onClose = { popToHub() },
        )

        AiRoute.Profile -> AiProfileScreen(
            username = username, uid = myUid, stats = stats, recent = recent,
            onOpenChallenge = { openChallenge(it) },
            onSignOut = { AuthService.signOut(); popToHub(); authVersion++ },
            onClose = { pop() },
        )
    }
}
