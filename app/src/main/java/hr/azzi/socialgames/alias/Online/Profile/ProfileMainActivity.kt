package hr.azzi.socialgames.alias.Online.Profile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.github.loadingview.LoadingDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.UserLeader
import hr.azzi.socialgames.alias.Online.Models.UserManagerModel
import hr.azzi.socialgames.alias.R
import kotlinx.android.synthetic.main.activity_profile_main.*
import kotlinx.android.synthetic.main.activity_profile_main.backButton
import kotlin.math.max

class ProfileMainActivity : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    var userLeader: UserLeader? = null
    val dialog: LoadingDialog by lazy {
        LoadingDialog.get(this)
    }

    var canChange: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_main)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        loadData()

        observe()
    }

    fun observe() {

        applyButton.setOnClickListener {
            updateUser()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    fun updateUser() {
        val username = usernameEditText.text.toString()

        if (username.length == 0) {
            Toast.makeText(this, "enter correct username", Toast.LENGTH_LONG).show()
            return
        }

        if (username.startsWith("Guest")) {
            Toast.makeText(this, "username already exists", Toast.LENGTH_LONG).show()
            return
        }

        dialog.show()
        db
            .collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener {
                dialog.hide()
                val count = it?.documents?.count()

                if (count != null) {

                    if (count == 0) {
                        setUsername(username)
                    } else {
                        Toast.makeText(this, "username already exists", Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(this, "something went wrong, please try again", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                dialog.hide()
                Toast.makeText(this, "something went wrong, please try again", Toast.LENGTH_LONG).show()
            }
    }

    private fun setUsername(username: String) {
        val user = FirebaseAuth.getInstance().currentUser

        user?.let {
            val map = hashMapOf<String, Any>("username" to username, "canChangeUsername" to false )
            dialog.show()
            db
                .document("Users/" + it.uid)
                .update(map)
                .addOnSuccessListener {
                    dialog.hide()
                    canChange = false
                    updateButton()
                    UserManagerModel._username = username
                    Toast.makeText(this, "username updated", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    dialog.hide()
                    Toast.makeText(this, "something went wrong", Toast.LENGTH_LONG).show()
                }

        }

    }


    private fun loadData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            dialog.show()
            db
                .document("Users/" + user.uid)
                .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    dialog.hide()
                    if (documentSnapshot != null) {
                        var data = documentSnapshot.data

                        canChange = (data?.get("canChangeUsername") as? Boolean) ?: true

                        val username = (data?.get("username") as? String) ?: "Guest"
                        val scores = data?.get("scores") as? HashMap<String, Long>
                        val scoreValues = scores?.values
                        val totalScore = (scores?.values?.sum() ?: 0).toInt()
                        val count = max(scoreValues?.count() ?: 0, 0)
                        val avgScore = totalScore.toFloat() / count.toFloat()
                        val userLeader = UserLeader(username, totalScore, avgScore, scores, 0, true)
                        updateUI(userLeader)
                    }

                }
        }
    }

    private fun updateUI(userLeader: UserLeader) {
        this.userLeader = userLeader

        usernameEditText.setText(userLeader.username)
        totalScoreTV.text = "Total Score: ${userLeader.totalScore}"
        gamesPlayedTV.text = "Games Played: ${userLeader.scores?.count() ?: 0}"
        averageScoreTV.text = "Average Score: ${userLeader.avgScore}"

        updateButton()
    }

    private fun updateButton() {
        if (canChange) {
            applyButton.isEnabled = true
            applyButton.alpha = 1F
        } else {
            applyButton.isEnabled = false
            applyButton.alpha = 0.5F
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, ProfileMainActivity::class.java)
    }
}
