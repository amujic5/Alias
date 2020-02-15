package hr.azzi.socialgames.alias

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Adapters.OnlineGameAdapter
import hr.azzi.socialgames.alias.Models.OnlineGame
import kotlinx.android.synthetic.main.activity_online_dashboard.*

class OnlineDashboardActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    lateinit var adapter: OnlineGameAdapter
    val user = FirebaseAuth.getInstance().currentUser
    var onlineGame: OnlineGame? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_dashboard)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()


        observe()
    }

    override fun onResume() {
        super.onResume()
        createGameIfNeeded()
    }

    fun updateUI() {
        onlineGame?.let {
            playersCountTextView.text = "${it.user.size} players online"
        }
    }

    fun observe() {
        backButton.setOnClickListener {
            this.finish()
        }

        db.collection("Games")
            .whereIn("status", arrayListOf("waiting", "playing"))
            .whereEqualTo("dictionary", "CRO")
            .limit(1)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                val documents = querySnapshot?.documents
                if (documents != null && !documents.isEmpty()) {
                    val data = documents.first().data
                    if (data != null) {
                        val onlineGame = OnlineGame.gameWithDictionary(data)
                        this.onlineGame = onlineGame
                        updateUI()
                    }
                }

                createGameIfNeeded()

            }

        onlinePlayButton.setOnClickListener {
            onlineGame?.let {
                val intent = OnlinePlayActivity.createIntent(this, it)
                startActivity(intent)
            }
        }

    }

    fun createGameIfNeeded() {

        val constantRef = db.collection("Constant").document("1")

        db.runTransaction { transaction ->
            val constantSnapshot = transaction.get(constantRef)
            val gameId = constantSnapshot.getString("gameId")?.toInt() ?: 0

            val lastGameRef = db.collection("Games").document(gameId.toString())
            val gameSnapshot = transaction.get(lastGameRef)


            val onlineGame = OnlineGame.gameWithDictionary(gameSnapshot.data!!)

            if (onlineGame.isFinished()) {
                val newGameIdString = (gameId + 1).toString()
                transaction.update(constantRef, "gameId", newGameIdString)

                val newGameRef = db.collection("Games").document(newGameIdString)
                val newGameMap = hashMapOf<String, Any>(
                    "dictionary" to "CRO",
                    "status" to "waiting",
                    "id" to newGameIdString
                )
                transaction.set(newGameRef, newGameMap)

            }
            null
        }

    }
}

