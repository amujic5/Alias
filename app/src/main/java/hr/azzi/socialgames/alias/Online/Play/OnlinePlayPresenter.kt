package hr.azzi.socialgames.alias.Online.Play

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import hr.azzi.socialgames.alias.Models.OnlineGame
import hr.azzi.socialgames.alias.Online.Models.Author
import hr.azzi.socialgames.alias.Online.Models.Message
import hr.azzi.socialgames.alias.Online.Models.MessageType
import hr.azzi.socialgames.alias.OnlinePlayActivity
import java.util.*
import kotlin.collections.ArrayList

class OnlinePlayPresenter(val view: OnlinePlayActivity, var game: OnlineGame) {

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    lateinit var gameId: String
    var gameListener: ListenerRegistration? = null
    var messageListener: ListenerRegistration? = null

    fun didCreate() {
        gameId = game.id ?: ""

        observeGame()
        observeMessages()
    }

    fun removeListeners() {
        gameListener?.remove()
        messageListener?.remove()
    }


    fun observeGame() {
        gameListener =  db.collection("Games")
            .document(gameId)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                val data = querySnapshot?.data
                if (data != null) {
                    val onlineGame = OnlineGame.gameWithDictionary(data)
                    this.game = onlineGame
                    view.updateGame(onlineGame)
                }
            }
    }

    fun observeMessages() {
        messageListener = db.collection("Games/$gameId/Message")
            .orderBy("date")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                var messages = ArrayList<Message>()
                for (document in querySnapshot!!.documents) {
                    val data = document.data!!
                    val username = data.get("user") as String
                    val text = data.get("text") as String
                    val messageType = data.get("messageType") as String

                    val timestamp = data.get("date") as Timestamp
                    val date = timestamp.toDate() ?: Date()

                    val message = Message(document.id, date, Author(username, username,null), text, MessageType.initFrom(messageType))
                    messages.add(message)
                }
                view.updateUI(messages)
            }
    }

    fun joinGame() {
        val username = view.username

        val gameRef = db.collection("Games").document(gameId)
        db.runTransaction {
            val snapshot = it.get(gameRef)
            val onlineGame = OnlineGame.gameWithDictionary(snapshot.data ?: hashMapOf())

            if (!onlineGame.user.contains(username)) {
                onlineGame.user.add(username)
                it.update(gameRef, "user", onlineGame.user)

                if (onlineGame.user.size == 2) {
                    it.update(gameRef, "admin", username)
                }

                if (onlineGame.user.size == 2) {
                    onlineGame.date = Timestamp(Date(Timestamp.now().toDate().time + (30 * 1000)))
                    it.update(gameRef, "date", onlineGame.date)
                }
            }

        }
    }

    fun leaveGame() {
        val username = view.username
        val gameRef = db.collection("Games").document(gameId)
        db.runTransaction {
            val snapshot = it.get(gameRef)
            val onlineGame = OnlineGame.gameWithDictionary(snapshot.data ?: hashMapOf())

            if (onlineGame.user.contains(username)) {
                onlineGame.user.remove(username)
                it.update(gameRef, "user", onlineGame.user)

                if (onlineGame.admin == username) {
                    it.update(gameRef, "admin", null)
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

    fun updateScore(correctCount: Int) {
        val username = view.username
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

    fun updateAdminScore(explainedCount: Int) {
        val admin = view.admin
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

    fun updateGameStatus(status: String) {
        if (game.status != status) {
            db.collection("Games")
                .document(gameId)
                .update("status", status)
        }
    }

}