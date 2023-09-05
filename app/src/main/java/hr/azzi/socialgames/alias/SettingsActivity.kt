package hr.azzi.socialgames.alias

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import hr.azzi.socialgames.alias.Adapters.FlagAdapter
import hr.azzi.socialgames.alias.Adapters.FlagAdapterDelegate
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.Models.FlagModel
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.databinding.ActivitySettingsBinding
import me.samlss.lighter.Lighter


class SettingsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, FlagAdapterDelegate {

    private lateinit var binding: ActivitySettingsBinding

    var adapter: FlagAdapter = FlagAdapter(ArrayList())
    var time: Int = 60
    var score: Int = 100

    var dictionaries: MutableList<DictionaryModel> = mutableListOf()

    val flags by lazy {
        ArrayList(dictionaries.toList().map { FlagModel(it.imageURLString, false, it.languageCode, it.language) })
    }

    val teams by lazy {
        intent.getParcelableArrayListExtra<Team>("playingTeams")
    }

    val preferences: SharedPreferences by lazy {
        this.getSharedPreferences("settings-recording", Context.MODE_PRIVATE)
    }

    val keyRecording = "settings-recording"
    var lighter: Lighter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        val layoutManager = GridLayoutManager(this, 4)
        binding.recyclerView.layoutManager = layoutManager

        loadData()
        binding.scrollView.smoothScrollTo(0,0)
    }

    fun loadData() {
        dictionaries = DictionaryService.instance.getDictionaries(this)
        flags[0].selected = true

        adapter = FlagAdapter(flags)
        binding.recyclerView.adapter = adapter
        adapter.delegate = this

        updateVsTextView()
        observe()
    }

    override fun onBackPressed() {
        if (this.lighter?.isShowing ?: false) {
            this.lighter?.dismiss()
            this.lighter = null
        } else {
            finish()
        }
    }

    fun observe() {
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.scoreSeekBar.setOnSeekBarChangeListener(this)
        binding.timeSeekBar.setOnSeekBarChangeListener(this)

        binding.playButton.setOnClickListener {
            play()
        }
    }

    fun play() {

        var index = flags.indexOfFirst { it.selected }
        val selectedDictionary = dictionaries[index]


        val newGame = Game(
            false,
            time,
            score,
            teams as ArrayList<Team>,
            ArrayList(selectedDictionary.words),
            0
        )
        DictionaryService.playingDictionary = selectedDictionary

        val intent =  Intent(this, PlayActivity::class.java)
        intent.putExtra("game", newGame)
        startActivity(intent)
    }


    fun updateVsTextView() {
        teams?.let {
            binding.vsTextView.setText(it.filter { it.playing }.map { it.teamName }.joinToString(" VS "))
        }

    }

    // OnSeekBarChangeListener

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (seekBar == binding.scoreSeekBar) {
            val score = progress + 40
            binding.scoreTextView.text = "$score"
            this.score = score
        } else if (seekBar == binding.timeSeekBar) {
            val time = progress + 20
            binding.timeTextView.text = "$time"
            this.time = time
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    // FlagAdapterDelegate
    override fun didClick(position: Int) {
        if (position < 0 || position >= flags.size) {
            return
        }
        flags.forEach {
            it.selected = false
        }
        flags[position].selected = true
        adapter.notifyDataSetChanged()

        binding.dictionaryTextView.text = flags[position].name
    }

}
