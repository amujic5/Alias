package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.ui.screens.WinnerScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class WinnerActivity : BaseActivity() {

    private val game: Game by lazy { intent.getParcelableExtra<Game>("game") as Game }
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        setContent {
            AliasTheme {
                WinnerScreen(
                    winnerName = game.winnerTeam?.teamName ?: "",
                    onShare = { share() },
                    onFinish = { finish() },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    private fun share() {
        var teamResults = ""
        game.sortedTeams.forEach {
            teamResults = "$teamResults \n team: ${it.teamName} (${it.firstPlayer} & ${it.secondPlayer}) score: ${it.getScore()}"
        }
        val localIntent = Intent(Intent.ACTION_SEND)
        localIntent.type = "text/plain"
        localIntent.putExtra(
            Intent.EXTRA_TEXT,
            "Hey there, we are playing Alias!!! Team: ${game.winnerTeam?.teamName} has won. Here are rest of the results:\n $teamResults \n" +
                " https://play.google.com/store/apps/details?id=hr.azzi.socialgames.alias"
        )
        startActivity(Intent.createChooser(localIntent, "Share To.."))
        logShare()
    }

    private fun logShare() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Video list")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Video list")
        firebaseAnalytics.logEvent("share_winner", bundle)
    }
}
