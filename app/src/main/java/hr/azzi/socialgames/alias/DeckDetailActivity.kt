package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import hr.azzi.socialgames.alias.Service.Billing
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.serializedName
import hr.azzi.socialgames.alias.ui.screens.DeckDetailScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

/** Deck detail + checkout. Free/owned decks Play; locked paid decks buy via Play Billing. */
class DeckDetailActivity : BaseActivity() {

    private val boardGame: BoardGame by lazy { intent.getParcelableExtra("boardGame")!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasTheme {
                DeckDetailScreen(
                    deck = boardGame,
                    onBack = { finish() },
                    onPlay = {
                        startActivity(
                            Intent(this, NewGameActivity::class.java).putExtra("boardGame", boardGame)
                        )
                    },
                    onBuy = { Billing.launchPurchase(this, boardGame.id.serializedName()) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reflect purchases made outside the app (or just completed) as soon as we return.
        Billing.refresh()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }
}
