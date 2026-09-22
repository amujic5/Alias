package hr.azzi.socialgames.alias

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import hr.azzi.socialgames.alias.arena.AliasAvatar
import hr.azzi.socialgames.alias.arena.AppAppearance
import hr.azzi.socialgames.alias.arena.ArenaColors
import hr.azzi.socialgames.alias.arena.ArenaProfileStore
import hr.azzi.socialgames.alias.arena.ArenaStore
import hr.azzi.socialgames.alias.arena.LocalAppAppearance
import hr.azzi.socialgames.alias.arena.ui.ArenaAvatarPicker
import hr.azzi.socialgames.alias.arena.ui.ArenaFlowScreen
import hr.azzi.socialgames.alias.arena.ui.ArenaModeScreen
import hr.azzi.socialgames.alias.arena.ui.ArenaPlayerProfileScreen
import hr.azzi.socialgames.alias.arena.ui.ArenaRewardGridScreen
import hr.azzi.socialgames.alias.arena.ui.ArenaStandingsScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme
import kotlinx.coroutines.launch

/**
 * The Arena, from the mode screen inwards.
 *
 * Its own activity rather than a route inside the AI hub: the Arena keeps a round clock
 * and a Firestore listener alive for as long as it is open, and an activity is the one
 * scope on Android that says plainly when that is. Leaving it stops both.
 */
class ArenaActivity : BaseActivity() {

    private val store: ArenaStore by viewModels()
    private val profile: ArenaProfileStore by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile.start()

        setComposeContent {
            AliasTheme {
                val appearance = AppAppearance.load(this)
                CompositionLocalProvider(LocalAppAppearance provides appearance.value) {
                    Box(Modifier.fillMaxSize().background(ArenaColors.backdrop).systemBarsPadding()) {
                        ArenaRoot(store, profile, onExit = { finish() })
                    }
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }
}

/** Where in the Arena we are: the two choices, then the round loop. */
private sealed interface ArenaStage {
    data object Mode : ArenaStage
    data object Live : ArenaStage
    data object Profile : ArenaStage
    data object Standings : ArenaStage
    data object Rewards : ArenaStage
    data object AvatarPicker : ArenaStage
}

@Composable
private fun ArenaRoot(store: ArenaStore, profile: ArenaProfileStore, onExit: () -> Unit) {
    var stage by remember { mutableStateOf<ArenaStage>(ArenaStage.Mode) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val mode by store.mode.collectAsState()
    val lang by store.lang.collectAsState()
    val avatar by profile.avatar.collectAsState()
    val summary by profile.summary.collectAsState()
    val name by profile.name.collectAsState()

    when (stage) {
        ArenaStage.Mode -> ArenaModeScreen(
            mode = mode,
            lang = lang,
            onMode = { store.setMode(it) },
            onLanguage = { store.setLanguage(it) },
            onEnter = { stage = ArenaStage.Live },
            onBack = onExit,
        )

        ArenaStage.Live -> ArenaFlowScreen(
            store = store,
            profile = profile,
            onClose = { stage = ArenaStage.Mode },
        )

        ArenaStage.Profile -> ArenaPlayerProfileScreen(
            profile = profile,
            stats = hr.azzi.socialgames.alias.ai.AIUserStats(0, 0, 0),
            recent = emptyList(),
            isAnonymous = hr.azzi.socialgames.alias.arena.ArenaProfile.isGuestName(name),
            onPickAvatar = { stage = ArenaStage.AvatarPicker },
            onOpenRewards = { stage = ArenaStage.Rewards },
            onOpenStandings = { stage = ArenaStage.Standings },
            onOpenChallenge = {},
            onSeeAll = {},
            onSignIn = {},
            onSignOut = {},
            onClose = { stage = ArenaStage.Live },
        )

        ArenaStage.Standings -> ArenaStandingsScreen(profile) { stage = ArenaStage.Profile }

        ArenaStage.Rewards -> ArenaRewardGridScreen(summary) { stage = ArenaStage.Profile }

        ArenaStage.AvatarPicker -> ArenaAvatarPicker(
            selected = AliasAvatar.named(avatar),
            onPick = { scope.launch { profile.setAvatar(it.id) } },
            onClose = { stage = ArenaStage.Profile },
        )
    }
}
