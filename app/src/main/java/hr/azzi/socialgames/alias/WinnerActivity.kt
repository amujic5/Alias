package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Models.Game

import kotlinx.android.synthetic.main.activity_winner.*

class WinnerActivity : AppCompatActivity() {

    val game: Game by lazy {
        intent.getParcelableExtra<Game>("game") as Game
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_winner)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        textView16.text = game.winnerTeam?.teamName

        observe()
    }

    fun observe() {
        finishButton.setOnClickListener {
            finish()
        }

        shareButton.setOnClickListener {

            var teamResults = ""
            game.sortedTeams.forEach {
                teamResults = "$teamResults \n team: ${it.teamName} (${it.firstPlayer} & ${it.secondPlayer}) score: ${it.getScore()}"
            }

            val localIntent = Intent("android.intent.action.SEND")
            localIntent.type = "text/plain"
            localIntent.putExtra(
                "android.intent.extra.TEXT",
                "Hey there, we are playing Alias!!! Team: ${game.winnerTeam?.teamName} has won. Here are rest of the results:\n $teamResults \n" + " https://play.google.com/store/apps/details?id=hr.azzi.socialgames.alias"
            )
            startActivity(Intent.createChooser(localIntent, "Share To.."))

            logShare()
        }
    }

    fun logShare() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Video list")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Video list")
        firebaseAnalytics.logEvent("share_winner", bundle)
    }


}
