package hr.azzi.socialgames.alias

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import hr.azzi.socialgames.alias.Adapters.TeamScoreAdapter
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.TeamScoreItem
import kotlinx.android.synthetic.main.activity_play.correctTextView
import kotlinx.android.synthetic.main.activity_play.skipTextView
import kotlinx.android.synthetic.main.activity_results.*

class ResultsActivity : AppCompatActivity() {

    val game: Game by lazy {
        intent.getParcelableExtra("game") as Game
    }

    var teamScoreDataSource = ArrayList<TeamScoreItem>()

    val adapter by lazy {
        TeamScoreAdapter(this, teamScoreDataSource)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        game.currentTeamHasFinishedTheRound()


        val scores = game.sortedTeams.mapIndexed { index, team ->
            val winner = index == 0 && game.nextTeam == null
            TeamScoreItem(index + 1, team.teamName, team.getScore(), winner)
        }

        teamScoreDataSource = ArrayList(scores)

        resultListView.adapter = adapter

        updateUI()
        observe()
    }

    fun updateUI() {
        teamTextView.text = game.currentTeam.teamName
        val correct = game.currentCorrectAnswers
        correctTextView.text = "$correct correct"

        val skipped = game.currentSkipAnswers
        skipTextView.text = "$skipped skipped"

        nextTeamTextView.text = game.nextTeam?.teamName
        answeringTextView.text = game.nextAnsweringPlayerName
        explainingTextView.text = game.nextExplainingPlayerName

        if (game.nextTeam == null) {
            constraintLayout3.alpha = 0F
            finishButton.alpha = 1F
        } else {
            constraintLayout3.alpha = 1F
            finishButton.alpha = 0F
        }


    }

    fun observe() {
        startButton.setOnClickListener {
            game.newRound()
            val intent =  Intent(this, PlayActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("game", game)
            startActivity(intent)
            finish()
        }

        finishButton.setOnClickListener {
            finish()
        }
    }

}
