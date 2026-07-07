package hr.azzi.socialgames.alias

import android.os.Bundle
import hr.azzi.socialgames.alias.ui.screens.online.OnlinePlayApp
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

/** Hosts the Online Play (realtime multiplayer) Compose flow. */
class OnlineActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
