package hr.azzi.socialgames.alias

import android.os.Bundle
import hr.azzi.socialgames.alias.online.OnlineAnalytics
import hr.azzi.socialgames.alias.ui.screens.online.OnlinePlayApp
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

/** Hosts the Online Play (realtime multiplayer) Compose flow. */
class OnlineActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh open — a recreation (rotation, process restart) isn't a new visit.
        if (savedInstanceState == null) OnlineAnalytics.screenOpen(this)
        setComposeContent {
            AliasTheme {
                OnlinePlayApp(onExit = { finish() })
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }
}
