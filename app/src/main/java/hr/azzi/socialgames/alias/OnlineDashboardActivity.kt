package hr.azzi.socialgames.alias

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Adapters.OnlineGameAdapter
import hr.azzi.socialgames.alias.Models.OnlineGame
import kotlinx.android.synthetic.main.activity_online_dashboard.*
import kotlin.collections.ArrayList

class OnlineDashboardActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    lateinit var adapter: OnlineGameAdapter
    var onlineGameDataSource = ArrayList<OnlineGame>()
    val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_dashboard)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        adapter = OnlineGameAdapter(this, onlineGameDataSource)
        listView.adapter = adapter

        observe()
        createGameIfNeeded()
    }

    fun observe() {
        backButton.setOnClickListener {
            this.finish()
        }

        db.collection("Games")
            .whereIn("status", arrayListOf("waiting", "playing"))
            .whereEqualTo("dictionary", "CRO")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                val documents = querySnapshot?.documents

                onlineGameDataSource.clear()
                for (document in querySnapshot?.documents!!.reversed()) {
                    val data = document.data!!
                    val onlineGame = OnlineGame.gameWithDictionary(data)
                    onlineGameDataSource.add(onlineGame)
                }
                adapter.notifyDataSetChanged()
            }

        listView.setOnItemClickListener { parent, view, position, id ->
            val onlineGame = onlineGameDataSource[position]

            val intent = OnlinePlayActivity.createIntent(this, onlineGame)
            startActivity(intent)
        }
    }

    fun createGameIfNeeded() {

        val constantRef = db.collection("Constant").document("1")

        db.runTransaction { transaction ->
            val constantSnapshot = transaction.get(constantRef)
            val gameId = constantSnapshot.getString("gameId")?.toInt() ?: 0

            val lastGameRef = db.collection("Games").document(gameId.toString())
            val gameSnapshot = transaction.get(lastGameRef)

            val status = gameSnapshot.getString("status")

            if (status != "waiting") {
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

