package hr.azzi.socialgames.alias

import android.os.Bundle
import hr.azzi.socialgames.alias.ui.screens.HowToPlayScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class HowToPlayActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent {
            AliasTheme {
                HowToPlayScreen(onGotIt = { finish() })
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }
}
