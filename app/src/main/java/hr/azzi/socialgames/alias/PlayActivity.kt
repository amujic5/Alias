package hr.azzi.socialgames.alias

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
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
import java.io.File

@SuppressLint("RestrictedApi, ClickableViewAccessibility")
class PlayActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd
    private lateinit var mInterstitialAd2: InterstitialAd

    var timer: CountDownTimer? = null
    var startTimer = 60
    var estimation = 60
    var isRunning = false

    var finishDialog: AlertDialog? = null
    var dialog: AlertDialog? = null

    val game: Game by lazy {
        intent.getParcelableExtra<Game>("game") as Game
    }

    val soundSystem: SoundSystem by lazy {
        SoundSystem(this)
    }

    var savedFile: File? = null
    var savedFiles: ArrayList<File> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        log()
        loadAd()
        loadIntrestialAd()
        if (game._currentTeamIndex % 2 == 1) {
            loadIntrestialAd2()
        }


        mInterstitialAd.setAdListener(object : AdListener() {
            override fun onAdClosed() {
                if (mInterstitialAd2.isLoaded) {
                    mInterstitialAd2.show()
                }
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        startTimer = game.time
        estimation = game.time
        timeTextView.text = "$estimation"


        observe()
        teamNameTextView.text = game.currentTeam.teamName
        showNewWord()
        updateLables()
    }

    var animationFlag = true
    fun startAnimation() {
        var size = Point()
        windowManager.defaultDisplay.getSize(size)
        if (animationFlag) {
            animationView.translationX = -size.x.toFloat()
            animationFlag = false
        }

        animationView.animate().setDuration(estimation.toLong()*1000).translationX(0f).start()
    }

    fun pauseAnimation() {
        animationView.animate().cancel()
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

    fun loadIntrestialAd2() {
        MobileAds.initialize(this) {}
        mInterstitialAd2 = InterstitialAd(this)
        val prod = "ca-app-pub-1489905432577426/4633082957"
        val test =  "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd2.adUnitId = prod
        mInterstitialAd2.loadAd(AdRequest.Builder().build())
    }

    fun log() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)


        firebaseAnalytics.setCurrentScreen(this, "PlayActivity", null /* class override */)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        bundle.putString("language", DictionaryService.playingDictionary?.language)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        logLanguage()
    }

    fun logLanguage() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "PlayActivity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "PlayActivity")
        val language = DictionaryService.playingDictionary?.language ?: "unknown"
        firebaseAnalytics.logEvent(language, bundle)
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
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        this.dialog = dialog


        val window = dialog.window
        var param = dialog.window?.attributes
        param?.gravity = Gravity.TOP
        param?.y = convertDpToPx(this, 80.toFloat()).toInt()
        window?.setAttributes(param);

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
        pauseAnimation()
        timer?.cancel()
        isRunning = false
    }

    private fun resume() {
        startAnimation()
        isRunning = true
        timer = object: CountDownTimer((estimation * 10000).toLong(), 1000) {
            @SuppressLint("RestrictedApi")
            override fun onTick(millisUntilFinished: Long) {

                if (!isRunning) {
                    cancel()
                    return
                }
                if (estimation < 10) {
                    soundSystem.playTikTok()
                }

                estimation--
                if (estimation < 0) {
                    estimation = 0
                }
                timeTextView.text = "$estimation"

                if (estimation == 0) {
                    soundSystem.playEnd()
                    isRunning = false
                    cancel()
                    Handler().postDelayed({
                        showResults()
                    }, 1000)

                } else {
                    if (estimation % 5 == 0) {
                        loadAd()
                    }
                }
            }

            override fun onFinish() {

            }
        }
        timer?.start()

    }

    fun showResults() {
        val intent =  Intent(this, ResultsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("game", game)

        if (savedFile != null) {
            val uriString = Uri.fromFile(savedFile).toString()
            intent.putExtra("fileURIString", uriString)
        }

        val fileStrings = savedFiles.map {
            Uri.fromFile(it).toString()
        }
        intent.putStringArrayListExtra("fileURIStrings", ArrayList(fileStrings))
        intent.flags = FLAG_ACTIVITY_NO_ANIMATION
        startActivity(intent)

        finish()
        if (mInterstitialAd.isLoaded) {
            Handler().postDelayed({
                mInterstitialAd.show()
            }, 1000)
        }
    }

}

fun convertDpToPx(context: Context, dp: Float): Float {
    return dp * context.getResources().getDisplayMetrics().density
}