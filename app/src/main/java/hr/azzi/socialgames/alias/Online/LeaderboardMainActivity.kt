package hr.azzi.socialgames.alias.Online

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.loadingview.LoadingDialog
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.UserLeader
import hr.azzi.socialgames.alias.Online.Models.UserManagerModel
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.activity_leaderboard_main.*
import kotlin.math.max

class LeaderboardMainActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    val myUsername = UserManagerModel.username()
    val dialog: LoadingDialog by lazy {
        LoadingDialog.get(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard_main)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        loadData()
        observe()
    }

    fun observe() {
        backButton.setOnClickListener {
            finish()
        }
    }

    fun loadData() {
        dialog.show()
        db
            .collection("Users")
            .get()
            .addOnSuccessListener {
                dialog.hide()
                if (it != null) {
                    var users = it.documents.map {
                        var data = it.data
                        val username = (data?.get("username") as? String) ?: "Guest"
                        val scores = data?.get("scores") as? HashMap<String, Long>
                        val scoreValues = scores?.values
                        val totalScore = (scores?.values?.sum() ?: 0).toInt()
                        val isMe = username == myUsername
                        val count = max(scoreValues?.count() ?: 0, 0)
                        val avgScore = totalScore.toFloat() / count.toFloat()
                        UserLeader(username, totalScore, avgScore, scores, 0, isMe)
                    }

                    users = users.sortedByDescending {
                        it.totalScore
                    }

                    users.forEachIndexed { index, userLeader ->
                        userLeader.index = index + 1
                    }

                    users.find {
                        it.isMe
                    }?.let {
                        updateMe(it)
                    }

                    val adapter = LeaderboardAdapter(this, ArrayList(users))
                    leaderboardListView.adapter = adapter

                }
            }
    }

    fun updateMe(userLeader: UserLeader) {
        numberTextView.text = userLeader.index.toString()
        teamTextView.text = userLeader.username
        scoreTextView.text = "Score: " + userLeader.totalScore
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, LeaderboardMainActivity::class.java)
    }
}
