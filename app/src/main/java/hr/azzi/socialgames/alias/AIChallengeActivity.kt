package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import hr.azzi.socialgames.alias.ai.AIAnalytics
import hr.azzi.socialgames.alias.ui.screens.ai.AiChallengeApp
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class AIChallengeActivity : BaseActivity() {

    /** The Arena's profile, so the hub shows the same person the Arena does. */
    private val arenaProfile: hr.azzi.socialgames.alias.arena.ArenaProfileStore by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = challengeIdFrom(intent)
        // Only on a fresh open — a recreation (rotation, process restart) isn't a new visit.
        if (savedInstanceState == null) AIAnalytics.screenOpen(this)
        // Log how the app was opened when arriving via a challenge deep link.
        if (id != null) when (intent?.data?.scheme?.lowercase()) {
            "https" -> AIAnalytics.appOpenViaUniversalLink(this)
            "aliaswords" -> AIAnalytics.appOpenViaScheme(this)
        }
        arenaProfile.start()
        setComposeContent {
            AliasTheme {
                AiChallengeApp(initialChallengeId = id, arenaProfile = arenaProfile, onExit = { finish() })
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    /** Parses https://aliaswords.com/challenge/{id}, aliaswords://challenge/{id}, or an extra. */
    private fun challengeIdFrom(intent: Intent?): String? {
        intent?.data?.let { uri ->
            val segs = uri.pathSegments ?: emptyList()
            val i = segs.indexOf("challenge")
            if (i >= 0 && i + 1 < segs.size) return segs[i + 1]
            if (uri.host == "challenge" && segs.isNotEmpty()) return segs[0]
        }
        return intent?.getStringExtra("challengeId")
    }
}
