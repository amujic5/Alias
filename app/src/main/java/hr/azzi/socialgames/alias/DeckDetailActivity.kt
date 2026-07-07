package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
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
        // Warm up a rewarded ad for the "Try for free" path on locked paid decks.
        if (!boardGame.free && !Billing.isUnlocked(boardGame.id.serializedName())) {
            TryFreeAd.preload(this)
        }
        val launchGame = {
            startActivity(
                Intent(this, NewGameActivity::class.java).putExtra("boardGame", boardGame)
            )
        }
        setComposeContent {
            AliasTheme {
                DeckDetailScreen(
                    deck = boardGame,
                    onBack = { finish() },
                    onPlay = { launchGame() },
                    onBuy = { Billing.launchPurchase(this, boardGame.id.serializedName()) },
                    // Watch the rewarded ad, then drop into the game for this session.
                    onTryFree = { TryFreeAd.show(this) { launchGame() } },
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
