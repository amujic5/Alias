package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hr.azzi.socialgames.alias.Adapters.WordAdapter
import hr.azzi.socialgames.alias.Adapters.WordAdapterDelegate
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.databinding.ActivityEditAnswersBinding

class EditAnswersActivity : AppCompatActivity(), WordAdapterDelegate {


    val adapter by lazy {
        WordAdapter(this, markedWords)
    }

    val game: Game by lazy {
        intent.getParcelableExtra<Game>("game") as Game
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

    private lateinit var binding : ActivityEditAnswersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        val layout = layoutInflater.inflate(R.layout.empty_view, binding.listView, false)
        binding.listView.addFooterView(layout)

        val words = game.currentTeamMarkedWords.map {
            MarkedWord(it.word, it.isCorrect)
        }
        markedWords = ArrayList(words)

        binding.listView.adapter = adapter
        adapter.delegate = this

        updateUI()
        observe()
    }

    fun observe() {
        binding.closeButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            markedWords

            val intent = Intent()
            intent.putExtra("words", markedWords)
            setResult(1, intent)
            finish()
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

    }

    fun updateUI() {
        binding.teamTextView.text = game.currentTeam.teamName
        binding.correctTextView.text = "$currentCorrectAnswers " + resources.getString(R.string.corrected)
        binding.skipTextView.text = "$currentSkipAnswers " + resources.getString(R.string.skipped)
    }

    override fun didChangeSwitchValue(isChecked: Boolean, position: Int) {
        markedWords[position].isCorrect = isChecked
        updateUI()
    }
}
