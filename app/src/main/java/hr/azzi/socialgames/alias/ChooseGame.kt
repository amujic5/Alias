package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.ui.screens.ChooseGameScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class ChooseGame : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasTheme {
                ChooseGameScreen(
                    decks = JSONService.boardGames,
                    onBack = { finish() },
                    onPick = { boardGame ->
                        startActivity(Intent(this, NewGameActivity::class.java).putExtra("boardGame", boardGame))
                    },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }
}