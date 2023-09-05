package hr.azzi.socialgames.alias

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import hr.azzi.socialgames.alias.Adapters.TeamScoreAdapter
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.Models.TeamScoreItem
import hr.azzi.socialgames.alias.databinding.ActivityResultsBinding
import java.io.File



class ResultsActivity : AppCompatActivity() {

    private lateinit var binding : ActivityResultsBinding

    val game: Game by lazy {
        intent.getParcelableExtra<Game>("game") as Game
    }
    val preferences: SharedPreferences by lazy {
        this.getSharedPreferences("settings-recording", Context.MODE_PRIVATE)
    }
    val keyRecording = "settings-recording-2"

    var teamScoreDataSource = ArrayList<TeamScoreItem>()
    var shouldShowEditAnswers: Boolean = true

    lateinit var adapter: TeamScoreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        game.currentTeamHasFinishedTheRound()
        reload()
        observe()
    }

    override fun onResume() {
        super.onResume()
        if (shouldShowEditAnswers) {
            shouldShowEditAnswers = false
            val editAnswerIntent =  Intent(this, EditAnswersActivity::class.java)
            editAnswerIntent.putExtra("game", game)
            startActivityForResult(editAnswerIntent, 1)
        }

    }

    fun reload() {
        val scores = game.sortedTeams.mapIndexed { index, team ->
            val winner = index == 0 && game.nextTeam == null
            TeamScoreItem(index + 1, team.teamName, team.getScore(), winner)
        }

        teamScoreDataSource = ArrayList(scores)

        adapter = TeamScoreAdapter(this, teamScoreDataSource)
        binding.resultListView.adapter = adapter
        adapter.notifyDataSetChanged()

        updateUI()
    }

    fun updateUI() {
        binding.teamTextView.text = game.currentTeam.teamName
        val correct = game.currentCorrectAnswers
        binding.correctTextView.text = "$correct " + resources.getString(R.string.corrected)

        val skipped = game.currentSkipAnswers
        binding.skipTextView.text = "$skipped " + resources.getString(R.string.skipped)

        binding.nextTeamTextView.text = game.nextTeam?.teamName
        binding.answeringTextView.text = game.nextAnsweringPlayerName
        binding.explainingTextView.text = game.nextExplainingPlayerName

        if (game.nextTeam == null) {
            binding.constraintLayout3.alpha = 0F
            binding.constraintLayout3.visibility = View.GONE
            binding.finishButton.alpha = 1F
            binding.finishButton.visibility = View.VISIBLE
        } else {
            binding.constraintLayout3.alpha = 1F
            binding.constraintLayout3.visibility = View.VISIBLE
            binding.finishButton.alpha = 0F
            binding.finishButton.visibility = View.GONE
        }

    }

    fun observe() {
        binding.startButton.setOnClickListener {
            deleteFile()
            game.newRound()
            val intent =  Intent(this, PlayActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("game", game)
            startActivity(intent)
            finish()
        }

        binding.finishButton.setOnClickListener {
            deleteFile()
            val intent =  Intent(this, WinnerActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("game", game)
            startActivity(intent)
            finish()
        }

        binding.editAnswersButton.setOnClickListener {
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


}
