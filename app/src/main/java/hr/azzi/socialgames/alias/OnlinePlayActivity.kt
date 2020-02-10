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
import com.google.firebase.auth.FirebaseAuth
import hr.azzi.socialgames.alias.Models.OnlineGame
import hr.azzi.socialgames.alias.Online.Adapters.InMessageViewHolder
import hr.azzi.socialgames.alias.Online.Adapters.OutMessageViewHolder
import hr.azzi.socialgames.alias.Online.Models.Author
import hr.azzi.socialgames.alias.Online.Models.Message
import hr.azzi.socialgames.alias.Online.Models.MessageType


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class OnlinePlayActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    val game: OnlineGame by lazy {
        intent.getParcelableExtra("game") as OnlineGame
    }


    lateinit var adapter: MessagesListAdapter<Message>
    lateinit var dictionary: DictionaryModel
    lateinit var words: ArrayList<String>

    var explainedCount = 0
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
        joinGame()
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
        startTextView.text = "Soon"

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
        input.setInputListener {
            createMessage(it.toString())
            true
        }

        db.collection("Games")
            .document(gameId)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                val data = querySnapshot?.data
                if (data != null) {
                    val timestamp = data.get("date") as? Timestamp
                    if (timestamp != null) {
                        this.startDate = timestamp.toDate()
                    }

                    val word = data.get("word") as? String
                    if (word != null) {
                        wordTextView.text = word
                        this.word = word
                    } else if (isAdmin()){
                        explainedCount += 1
                        updateAdminScore()
                        updateLables()
                        updateGame(newWord)
                    }

                }
            }

        db.collection("Games/$gameId/Message")
            .orderBy("date")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                adapter.clear(true)
                for (document in querySnapshot!!.documents) {
                    val data = document.data!!
                    val username = data.get("user") as String
                    val text = data.get("text") as String
                    val messageType = data.get("messageType") as String

                    val timestamp = data.get("date") as Timestamp
                    val date = timestamp.toDate() ?: Date()

                    val message = Message(document.id, date, Author(username, username,null), text, MessageType.initFrom(messageType))
                    adapter.addToStart(message, true)
                }
            }
    }

    fun initGame() {
        username = user?.displayName ?: "no_name"
        gameId = game?.id ?: "0"

        val dictionaries = DictionaryService.instance.getDictionaries(this)
        dictionary = dictionaries.first {
             it.languageCode == game.dictionary
        }
        words = ArrayList(dictionary.words)

        teamNameTextView.text = username

    }

    fun startTimer() {
        val timer = object: CountDownTimer((4 * 1000).toLong(), 1000) {
            @SuppressLint("RestrictedApi")
            override fun onTick(millisUntilFinished: Long) {

                updateTime()
            }

            override fun onFinish() {
                openResult()
            }
        }
        timer.start()
    }

    fun openResult() {
        val intent = OnlineResultActivity.createIntent(this, gameId)
        startActivity(intent)
    }

    fun updateTime() {
        val seconds = roundTime - ((Date().time - startDate.time)/1000).toInt()
        timeOnlineTextView.text = seconds.toString()
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

        val city = hashMapOf(
            "text" to messageText,
            "messageType" to messageType.text,
            "user" to username,
            "date" to Timestamp(Date())
        )

        db.collection("Games/$gameId/Message")
            .add(city)
            .addOnSuccessListener { Log.d("", "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w("", "Error writing document", e) }

        if (messageType == MessageType.CORRECT) {
            correctCount += 1
            updateScore()
            updateLables()
            clearWord(messageText)
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
        return normalize(text) == normalize(this.word)
    }

    fun normalize(text: String): String {
        return text
            .toLowerCase()
            .replace("č", "c")
            .replace("đ", "d")
            .replace("š", "s")
            .replace("ž", "z")
            .replace("ć", "c")
            .replace("-", " ")
            .trim()
    }

    // service calls
    fun joinGame() {
        val gameRef = db.collection("Games").document(gameId)
        db.runTransaction {
            val snapshot = it.get(gameRef)
            val onlineGame = OnlineGame.gameWithDictionary(snapshot.data ?: hashMapOf())

            if (!onlineGame.user.contains(username)) {
                onlineGame.user.add(username)
                it.update(gameRef, "user", onlineGame.user)

                if (onlineGame.user.size == 2) {
                    it.update(gameRef, "admin", username)
                    admin = username
                    updateAdminView()
                }
            }

        }
    }

    fun clearWord(correctWord: String) {
        val gameRef = db.collection("Games")
            .document(gameId)
        db.runTransaction {
            val snapshot = it.get(gameRef)
            val word = (snapshot.get("word") as? String) ?: ""
            if (normalize(word) == normalize(correctWord)) {
                it.update(gameRef, "word", null)
            }
        }
    }

    fun updateGame(word: String?) {
        db
            .collection("Games")
            .document(gameId)
            .update("word", word)
            .addOnCompleteListener {
                print("done")
            }
    }

    fun updateScore() {
        val score = hashMapOf(
            "score" to correctCount,
            "admin" to false
        )
        db
            .collection("Games/$gameId/Score")
            .document(username)
            .set(score)
            .addOnCompleteListener {
                print("completed")
            }
    }

    fun updateAdminScore() {
        val score = hashMapOf(
            "score" to explainedCount,
            "admin" to true
        )
        db
            .collection("Games/$gameId/Score")
            .document(admin)
            .set(score)
            .addOnCompleteListener {
                print("completed")
            }
    }

    companion object {
        fun createIntent(context: Context, onlineGame: OnlineGame) = Intent(context, OnlinePlayActivity::class.java).apply {
            putExtra("game", onlineGame)
        }
    }

}

