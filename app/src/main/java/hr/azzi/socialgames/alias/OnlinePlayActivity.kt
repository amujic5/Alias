package hr.azzi.socialgames.alias

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_online_play.*
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.synthetic.main.activity_online_play.correctTextView
import kotlinx.android.synthetic.main.activity_online_play.dotTextView
import kotlinx.android.synthetic.main.activity_online_play.skipTextView
import kotlinx.android.synthetic.main.activity_online_play.wordTextView
import kotlin.random.Random
import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import com.google.firebase.auth.FirebaseAuth
import hr.azzi.socialgames.alias.Models.OnlineGame
import hr.azzi.socialgames.alias.Online.Adapters.InMessageViewHolder
import hr.azzi.socialgames.alias.Online.Adapters.OutMessageViewHolder
import hr.azzi.socialgames.alias.Online.Models.Author
import hr.azzi.socialgames.alias.Online.Models.Message
import hr.azzi.socialgames.alias.Online.Models.MessageType
import hr.azzi.socialgames.alias.Online.Play.OnlinePlayPresenter
import kotlinx.android.synthetic.main.activity_home.*
import kotlin.collections.ArrayList


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class OnlinePlayActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

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

            val randomIndex = randomValue(0, words.size - 1)
            val wordString = words[randomIndex]
            words.removeAt(randomIndex)

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
        username = user?.displayName ?: "no_name"
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
        val intent = OnlineResultActivity.createIntent(this, gameId)
        startActivity(intent)
        finish()
    }

    fun updateTime() {
        val seconds = roundTime - ((Date().time - startDate.time)/1000).toInt()
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
            var skipped = explainedCount
            skipTextView.text = "$skipped " + resources.getString(R.string.skipped)
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

    // service calls


    companion object {
        fun createIntent(context: Context, onlineGame: OnlineGame) = Intent(context, OnlinePlayActivity::class.java).apply {
            putExtra("game", onlineGame)
        }
    }

}

