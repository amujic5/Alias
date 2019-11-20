package hr.azzi.socialgames.alias

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import hr.azzi.socialgames.alias.Models.Game
import hr.azzi.socialgames.alias.Models.MarkedWord
import hr.azzi.socialgames.alias.Service.SoundSystem
import kotlinx.android.synthetic.main.activity_play.*
import kotlinx.android.synthetic.main.dialog_play.view.*

class PlayActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd

    var timer: CountDownTimer? = null
    var estimation = 60
    var isRunning = false
    var correctCount = 0
    var skipCount = 0

    val game: Game by lazy {
        intent.getParcelableExtra("game") as Game
    }

    val soundSystem: SoundSystem by lazy {
        SoundSystem(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        log()
        loadAd()
        loadIntrestialAd()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        estimation = game.time

        observe()
        teamNameTextView.text = game.currentTeam.teamName
        showNewWord()
    }

    fun loadAd() {
        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun loadIntrestialAd() {
        MobileAds.initialize(this) {}
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    fun log() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)


        firebaseAnalytics.setCurrentScreen(this, "PlayActivity", null /* class override */)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        bundle.putString("language", game._dictionary.language)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun observe() {

        pauseButton.setOnClickListener {
            pause()
            showDialog()
        }

        correctButton.setOnClickListener {
            game.addMarkedWord(MarkedWord("", true))
            showNewWord()
            soundSystem.playRightButton()
            updateLables()
        }

        skipButton.setOnClickListener {
            game.addMarkedWord(MarkedWord("", false))
            showNewWord()
            soundSystem.playSkipButton()
            updateLables()
        }
    }

    fun showDialog() {
        val context = this
        val builder = AlertDialog.Builder(context)

        builder.setCancelable(false)

        val view = layoutInflater.inflate(R.layout.dialog_play, null)
        builder.setView(view)

        view.explainingTextView.text = game.explainingPlayerName
        view.answeringTextView.text = game.answeringPlayerName

        view.correctTextView.text = "$correctCount correct"
        view.skipTextView.text = "$skipCount skipped"
        view.teamTextView.text = game.currentTeam.teamName

        val dialog = builder.show()

        view.stopButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        view.playButton.setOnClickListener {
            dialog.dismiss()
            resume()
        }

    }

    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onResume() {
        super.onResume()
        showDialog()
    }

    override fun onBackPressed() {
        pause()
        showDialog()
    }

    fun updateLables() {
        val correct = game.currentCorrectAnswers
        correctTextView.text = "$correct correct"

        var skipped = game.currentSkipAnswers
        skipTextView.text = "$skipped skipped"
    }

    // WORD

    fun showNewWord() {
        wordTextView.text = game.newWord
    }

    // COUNTER
    private fun pause() {
        timer?.cancel()
        isRunning = false
    }

    private fun resume() {
        isRunning = true
        timer = object: CountDownTimer((estimation * 10000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {

                if (estimation < 10) {
                    soundSystem.playTikTok()
                }

                estimation--
                timeTextView.text = "$estimation"

                if (estimation == 0) {
                    soundSystem.playEnd()
                    isRunning = false
                    cancel()
                    showResults()
                }
            }

            override fun onFinish() {

            }
        }
        timer?.start()
    }

    fun showResults() {
        val intent =  Intent(this, ResultsActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("game", game)
        startActivity(intent)
        finish()
        mInterstitialAd.show()
    }


}
