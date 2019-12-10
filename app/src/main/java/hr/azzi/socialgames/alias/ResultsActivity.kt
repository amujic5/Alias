package hr.azzi.socialgames.alias

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import hr.azzi.socialgames.alias.Adapters.TeamScoreAdapter
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.Models.TeamScoreItem
import kotlinx.android.synthetic.main.activity_play.correctTextView
import kotlinx.android.synthetic.main.activity_play.skipTextView
import kotlinx.android.synthetic.main.activity_results.*
import java.io.File
import java.net.URI

class ResultsActivity : AppCompatActivity() {

    val game: Game by lazy {
        intent.getParcelableExtra("game") as Game
    }

    var teamScoreDataSource = ArrayList<TeamScoreItem>()

    lateinit var adapter: TeamScoreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        game.currentTeamHasFinishedTheRound()
        reload()
        observe()

        val fileURIString = intent.getStringExtra("fileURIString")
        if (fileURIString != null) {
            val uri = Uri.parse(fileURIString)
            createInstagramIntent("video/*", uri)
        }

//        val fileURIStrings = intent.getStringArrayListExtra("fileURIStrings")
//        val uris = fileURIStrings.map {
//            Uri.parse(it)
//        }
//        createInstagramIntents("video/*", ArrayList(uris))
    }

    override fun onBackPressed() {
    }

    fun reload() {
        val scores = game.sortedTeams.mapIndexed { index, team ->
            val winner = index == 0 && game.nextTeam == null
            TeamScoreItem(index + 1, team.teamName, team.getScore(), winner)
        }

        teamScoreDataSource = ArrayList(scores)

        adapter = TeamScoreAdapter(this, teamScoreDataSource)
        resultListView.adapter = adapter
        adapter.notifyDataSetChanged()

        updateUI()
    }

    fun updateUI() {
        teamTextView.text = game.currentTeam.teamName
        val correct = game.currentCorrectAnswers
        correctTextView.text = "$correct " + resources.getString(R.string.corrected)

        val skipped = game.currentSkipAnswers
        skipTextView.text = "$skipped " + resources.getString(R.string.skipped)

        nextTeamTextView.text = game.nextTeam?.teamName
        answeringTextView.text = game.nextAnsweringPlayerName
        explainingTextView.text = game.nextExplainingPlayerName

        if (game.nextTeam == null) {
            constraintLayout3.alpha = 0F
            constraintLayout3.visibility = View.GONE
            finishButton.alpha = 1F
            finishButton.visibility = View.VISIBLE
        } else {
            constraintLayout3.alpha = 1F
            constraintLayout3.visibility = View.VISIBLE
            finishButton.alpha = 0F
            finishButton.visibility = View.GONE
        }

    }

    fun observe() {
        startButton.setOnClickListener {
            deleteFile()
            game.newRound()
            val intent =  Intent(this, PlayActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("game", game)
            startActivity(intent)
            finish()
        }

        finishButton.setOnClickListener {
            deleteFile()
            finish()
        }

        editAnswersButton.setOnClickListener {
            val intent =  Intent(this, EditAnswersActivity::class.java)
            intent.putExtra("game", game)
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val words = data?.getParcelableArrayListExtra<MarkedWord>("words")

        if (words != null) {
            game.setTeamMarkedWords(words)
            reload()
        }


    }

    fun deleteFile() {
        val fileURIString = intent.getStringExtra("fileURIString")
        if (fileURIString != null) {
            val file = File(fileURIString)

            if (file != null) {
                if (file.exists()) {
                    file.delete()
                }
            }

        }
    }

    fun createInstagramIntent(type: String, uri: Uri) {
        val share = Intent(Intent.ACTION_SEND);

        share.type = type
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        share.putExtra(Intent.EXTRA_STREAM, uri)

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"))
    }

    fun createInstagramIntents(type: String, uris: ArrayList<Uri>) {
        val share = Intent(Intent.ACTION_SEND_MULTIPLE)

        share.type = type
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        share.putExtra(Intent.EXTRA_STREAM, uris)
        //share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"));
    }

}
