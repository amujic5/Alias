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
import hr.azzi.socialgames.alias.Service.DictionaryService
import hr.azzi.socialgames.alias.Service.SoundSystem
import kotlinx.android.synthetic.main.activity_play.*
import kotlinx.android.synthetic.main.dialog_play.view.*

class PlayActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd

    var timer: CountDownTimer? = null
    var estimation = 60
    var isRunning = false

    var finishDialog: AlertDialog? = null
    var dialog: AlertDialog? = null

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
        val prod = "ca-app-pub-1489905432577426/3906492992"
        val test =  "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.adUnitId = prod
        mInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    fun log() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)


        firebaseAnalytics.setCurrentScreen(this, "PlayActivity", null /* class override */)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        bundle.putString("language", DictionaryService.playingDictionary?.language)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun observe() {

        pauseButton.setOnClickListener {
            pause()
            showDialog()
        }

        correctButton.setOnClickListener {
            game.addMarkedWord(MarkedWord(wordTextView.text.toString(), true))
            showNewWord()
            soundSystem.playRightButton()
            updateLables()
        }

        skipButton.setOnClickListener {
            game.addMarkedWord(MarkedWord(wordTextView.text.toString(), false))
            showNewWord()
            soundSystem.playSkipButton()
            updateLables()
        }
    }

    fun showDialog() {
        this.dialog?.dismiss()
        this.finishDialog?.dismiss()

        val context = this
        val builder = AlertDialog.Builder(context)

        builder.setCancelable(false)

        val view = layoutInflater.inflate(R.layout.dialog_play, null)
        builder.setView(view)

        view.explainingTextView.text = game.explainingPlayerName
        view.answeringTextView.text = game.answeringPlayerName

        val correct = game.currentCorrectAnswers
        view.correctTextView.text = "$correct " + resources.getString(R.string.corrected)
        var skipped = game.currentSkipAnswers
        view.skipTextView.text = "$skipped " + resources.getString(R.string.skipped)
        view.teamTextView.text = game.currentTeam.teamName

        val dialog = builder.show()
        this.dialog = dialog

        view.stopButton.setOnClickListener {
            dialog.dismiss()
            showFinishDialog()
        }

        view.playButton.setOnClickListener {
            dialog.dismiss()
            resume()
        }

    }

    fun showFinishDialog() {
        this.dialog?.dismiss()
        this.finishDialog?.dismiss()

        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.warning))

        // Display a message on alert dialog
        builder.setMessage(getString(R.string.are_you_sure_quit))

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton(getString(R.string.yes)){ dialog, which ->
            finish()
        }


        // Display a negative button on alert dialog
        builder.setNegativeButton(getString(R.string.no)){ dialog, which ->
            showDialog()
        }

        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()

        this.finishDialog = dialog
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
        correctTextView.text = "$correct " + resources.getString(R.string.corrected)

        var skipped = game.currentSkipAnswers
        skipTextView.text = "$skipped " + resources.getString(R.string.skipped)
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

                if (!isRunning) {
                    cancel()
                    return
                }
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
        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        }
    }


}
