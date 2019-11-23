package hr.azzi.socialgames.alias

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import hr.azzi.socialgames.alias.Adapters.WordAdapter
import hr.azzi.socialgames.alias.Adapters.WordAdapterDelegate
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import kotlinx.android.synthetic.main.activity_edit_answers.*
import kotlinx.android.synthetic.main.activity_edit_answers.correctTextView
import kotlinx.android.synthetic.main.activity_edit_answers.skipTextView
import kotlinx.android.synthetic.main.activity_edit_answers.teamTextView


class EditAnswersActivity : AppCompatActivity(), WordAdapterDelegate {


    val adapter by lazy {
        WordAdapter(this, markedWords)
    }

    val game: Game by lazy {
        intent.getParcelableExtra("game") as Game
    }

    val currentCorrectAnswers: Int
        get() {
            return markedWords.filter {it.isCorrect}.size
        }

    val currentSkipAnswers: Int
        get() {
            return markedWords.filter {!it.isCorrect}.size
        }

    var markedWords: ArrayList<MarkedWord> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_answers)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        val words = game.currentTeamMarkedWords.map {
            MarkedWord(it.word, it.isCorrect)
        }
        markedWords = ArrayList(words)

        listView.adapter = adapter
        adapter.delegate = this

        updateUI()
        observe()
    }

    fun observe() {
        closeButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            markedWords

            val intent = Intent()
            intent.putExtra("words", markedWords)
            setResult(1, intent)
            finish()
        }

        cancelButton.setOnClickListener {
            finish()
        }

    }

    fun updateUI() {
        teamTextView.text = game.currentTeam.teamName
        correctTextView.text = "$currentCorrectAnswers " + R.string.corrected
        skipTextView.text = "$currentSkipAnswers " + R.string.skipped
    }

    override fun didChangeSwitchValue(isChecked: Boolean, position: Int) {
        markedWords[position].isCorrect = isChecked
        updateUI()
    }
}
