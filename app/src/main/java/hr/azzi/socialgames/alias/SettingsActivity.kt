package hr.azzi.socialgames.alias

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import hr.azzi.socialgames.alias.Adapters.FlagAdapter
import hr.azzi.socialgames.alias.Adapters.FlagAdapterDelegate
import hr.azzi.socialgames.alias.Models.Dictionary
import hr.azzi.socialgames.alias.Models.FlagModel
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.Team
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.RecordingFlag
import hr.azzi.socialgames.alias.Views.TipTextView
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.activity_settings.backButton
import kotlinx.android.synthetic.main.activity_settings.vsTextView
import kotlinx.android.synthetic.main.text_layout.view.*
import me.samlss.lighter.parameter.MarginOffset
import me.samlss.lighter.parameter.LighterParameter
import me.samlss.lighter.Lighter
import me.samlss.lighter.parameter.Direction
import me.samlss.lighter.shape.RectShape


class SettingsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, FlagAdapterDelegate {


    var adapter: FlagAdapter = FlagAdapter(ArrayList())
    var time: Int = 60
    var score: Int = 100

    var dictionaries: MutableList<Dictionary> = mutableListOf()

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
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        val layoutManager = GridLayoutManager(this, 4)
        recyclerView.layoutManager = layoutManager

        loadData()
        scrollView.smoothScrollTo(0,0)

        showInfo()
    }

    fun showInfo() {

        val shouldShowInfo = preferences.getBoolean(keyRecording, false)

        if (shouldShowInfo) {

            val lighter =  Lighter.with(this)
            this.lighter = lighter

            val lighterParamter= LighterParameter.Builder()
                .setHighlightedView(recordingCheckbox)
                .setTipLayoutId(R.layout.text_layout)
                .setLighterShape(RectShape(0f, 0f, 30f))
                .setTipViewRelativeDirection(Direction.TOP)
                .setTipViewRelativeOffset(MarginOffset(0, 0, 0, 10))
                .build()

            lighter
                .addHighlight(
                    lighterParamter
                )
                .show()

            preferences.edit().putBoolean(keyRecording, true).apply()
        }
    }

    fun loadData() {
        dictionaries = DictionaryService.instance.getDictionaries(this)
        flags[0].selected = true

        adapter = FlagAdapter(flags)
        recyclerView.adapter = adapter
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
        backButton.setOnClickListener {
            finish()
        }
        scoreSeekBar.setOnSeekBarChangeListener(this)
        timeSeekBar.setOnSeekBarChangeListener(this)

        playButton.setOnClickListener {
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
            teams,
            ArrayList(selectedDictionary.words),
            0
        )
        DictionaryService.playingDictionary = selectedDictionary

        RecordingFlag.recordingEnabled = recordingCheckbox.isChecked

        val intent =  Intent(this, PlayActivity::class.java)
        intent.putExtra("game", newGame)
        startActivity(intent)
    }


    fun updateVsTextView() {

        vsTextView.setText(teams.filter { it.playing }.map { it.teamName }.joinToString(" VS "))
    }

    // OnSeekBarChangeListener

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (seekBar == scoreSeekBar) {
            val score = progress + 60
            scoreTextView.text = "$score"
            this.score = score
        } else if (seekBar == timeSeekBar) {
            val time = progress + 20
            timeTextView.text = "$time"
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

        dictionaryTextView.text = flags[position].name
    }
}
