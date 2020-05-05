package hr.azzi.socialgames.alias

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_online_play.*
import com.stfalcon.chatkit.messages.MessagesListAdapter
import java.util.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.synthetic.main.activity_online_play.correctTextView
import kotlinx.android.synthetic.main.activity_online_play.dotTextView
import kotlinx.android.synthetic.main.activity_online_play.skipTextView
import kotlinx.android.synthetic.main.activity_online_play.wordTextView
import kotlin.random.Random
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import hr.azzi.socialgames.alias.Models.OnlineGame
import hr.azzi.socialgames.alias.Online.Adapters.InMessageViewHolder
import hr.azzi.socialgames.alias.Online.Adapters.OutMessageViewHolder
import hr.azzi.socialgames.alias.Online.Models.Message
import hr.azzi.socialgames.alias.Online.Models.MessageType
import hr.azzi.socialgames.alias.Online.Models.UserManagerModel
import hr.azzi.socialgames.alias.Online.Play.OnlinePlayPresenter
import kotlinx.android.synthetic.main.activity_online_play.adView
import kotlinx.android.synthetic.main.activity_online_play.teamNameTextView
import kotlin.collections.ArrayList


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class OnlinePlayActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd

    lateinit var game: OnlineGame
    lateinit var presenter: OnlinePlayPresenter

    lateinit var adapter: MessagesListAdapter<Message>
    lateinit var dictionary: DictionaryModel
    lateinit var words: ArrayList<String>

    var timer: CountDownTimer? = null

    var explainedCount = -1
    var correctCount = 0

    val newWord: String
        get() {

            var wordString: String = ""
            while (true) {
                val randomIndex = randomValue(0, words.size - 1)
                wordString = words[randomIndex]
                words.removeAt(randomIndex)

                if (wordString.split(" ").count() == 1) {
                    break
                }
            }

            return wordString
        }

    private fun randomValue(min: Int, max: Int) : Int {
        return Random.nextInt(min, max)
    }

    var word: String = ""
    lateinit var gameId: String
    lateinit var username: String
    var admin: String = "admin"
    var startDate: Date = Date()
    val roundTime = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_online_play)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        initGame()
        presenter = OnlinePlayPresenter(this, game)
        presenter.didCreate()
        presenter.joinGame()

        updateAdminView()
        updateIntroView()

        val holdersConfig = MessagesListAdapter.HoldersConfig()
        holdersConfig.setOutcomingTextConfig(OutMessageViewHolder::class.java, R.layout.item_outcoming_text_message)
        holdersConfig.setIncomingTextConfig(InMessageViewHolder::class.java, R.layout.item_incoming_text_message)
        adapter = MessagesListAdapter<Message>(username, holdersConfig, null)

        messagesList.setAdapter(adapter)

        startTimer()
        observe()

        loadIntrestialAd()
        loadAd()
        log()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.removeListeners()
    }
    fun loadAd() {
        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun loadIntrestialAd() {
        MobileAds.initialize(this) {}
        mInterstitialAd = InterstitialAd(this)
        val prod = "ca-app-pub-1489905432577426/6419779676"
        val test =  "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.adUnitId = prod
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        mInterstitialAd.adListener = object: AdListener() {
            override fun onAdClosed() {
                openNextScreen()
            }
        }
    }

    fun log() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "ONLINE PLAY")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "ONLINE PLAY")
        firebaseAnalytics.logEvent("ONLINEPLAY", bundle)
    }

    fun updateIntroView() {
        playersCountTextView.text = game.user.size.toString()
        languageTextView.text = dictionary.language
    }

    fun updateAdminView() {
        if (isAdmin()) {
            wordTextView.visibility = View.VISIBLE
        } else {
            wordTextView.visibility = View.INVISIBLE
            dotTextView.visibility = View.INVISIBLE
            skipTextView.visibility = View.INVISIBLE
        }
    }

    fun observe() {
        leaveButton.setOnClickListener {
            presenter.leaveGame()
            finish()
        }

        input.setInputListener {
            createMessage(it.toString())
            true
        }

    }

    fun initGame() {
        game = intent.getParcelableExtra("game") as OnlineGame
        username = UserManagerModel.username()
        gameId = game?.id ?: "0"

        val dictionaries = DictionaryService.instance.getDictionaries(this)
        dictionary = dictionaries.first {
             it.languageCode == game.dictionary
        }
        words = ArrayList(dictionary.words)

        teamNameTextView.text = username
    }

    fun updateGame(game: OnlineGame) {
        this.game = game

        this.startDate = game.date?.toDate() ?: Date()

        admin = game.admin ?: ""
        updateAdminView()

        val word = game.word

        if (word != null) {
            wordTextView.text = word
            this.word = word
        } else if (isAdmin()){
            explainedCount += 1
            presenter.updateAdminScore(explainedCount)
            updateLables()
            presenter.updateGame(newWord)
        }

        if (game.isPlaying()) {
            introContainer.visibility = GONE
        } else if (game.isWaiting()) {
            introContainer.visibility = VISIBLE
            updateIntroView()
        }
    }

    fun updateUI(messages: ArrayList<Message>) {
        adapter.clear(true)
        messages.forEach {
            adapter.addToStart(it, true)
        }
    }

    fun startTimer() {
        timer = object: CountDownTimer((1000 * 1000).toLong(), 1000) {
            @SuppressLint("RestrictedApi")
            override fun onTick(millisUntilFinished: Long) {

                updateTime()
            }

            override fun onFinish() {
            }

        }
        timer?.start()

    }

    fun openResult() {
        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            openNextScreen()
        }
    }

    fun openNextScreen() {
        val intent = OnlineResultActivity.createIntent(this, gameId, game)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }


    fun updateTime() {
        val seconds = roundTime - ((Date().time - startDate.time)/1000).toInt()
        if (seconds % 5 == 0) {
            loadAd()
        }
        timeOnlineTextView.text = seconds.toString()

        val gameStartDate = game.date

        if (gameStartDate != null) {
            val newSeconds = -((Date().time - gameStartDate.toDate().time)/1000).toInt()
            startTextView.text = "${newSeconds}"

            if (newSeconds < -59) {
                timer?.cancel()
                openResult()
                presenter.updateGameStatus("finished")
            } else if (newSeconds <= 0) {
                presenter.updateGameStatus("playing")
            }

        } else {
            startTextView.text = "Soon"
        }

    }

    fun updateLables() {
        if (isAdmin()) {
            var correct = explainedCount
            correctTextView.text = "$correct " + resources.getString(R.string.corrected)
        } else {
            val correct = correctCount
            correctTextView.text = "$correct " + resources.getString(R.string.corrected)
        }
    }

    fun isAdmin(): Boolean {
        return admin == username
    }

    fun createMessage(text: String) {
        val messageText = text.replace("\n", " ").trim()

        val messageType = messageType(messageText)

        val message = hashMapOf(
            "text" to messageText,
            "messageType" to messageType.text,
            "user" to username,
            "date" to Timestamp(Date())
        )

        if (messageType == MessageType.EXPLAIN) {

            val normalized = presenter.normalize(text).replace(" ", "")
            val thisWordNormalized = presenter.normalize(this.word).replace(" ", "")

            if (normalized.contains(thisWordNormalized) || thisWordNormalized.contains(normalized)) {
                Toast.makeText(this, "Can not use that explanation!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        db.collection("Games/$gameId/Message")
            .add(message)
            .addOnSuccessListener { Log.d("", "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w("", "Error writing document", e) }

        if (messageType == MessageType.CORRECT) {
            correctCount += 1
            presenter.updateScore(correctCount)
            updateLables()
            presenter.clearWord(messageText)
        }
    }

    fun messageType(text: String): MessageType {
        if (username == admin) {
            return MessageType.EXPLAIN
        }

        if (isCorrect(text)) {
            return MessageType.CORRECT
        } else {
            return MessageType.WRONG
        }
    }

    fun isCorrect(text: String): Boolean {
        return presenter.normalize(text) == presenter.normalize(this.word)
    }

    companion object {
        fun createIntent(context: Context, onlineGame: OnlineGame) = Intent(context, OnlinePlayActivity::class.java).apply {
            putExtra("game", onlineGame)
        }
    }

}

