package hr.azzi.socialgames.alias

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.github.loadingview.LoadingDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Online.Models.UserManagerModel
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.synthetic.main.activity_home.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class HomeActivity : AppCompatActivity() {

    val dialog: LoadingDialog by lazy {
        LoadingDialog.get(this)
    }

    val db = FirebaseFirestore.getInstance()
    val RC_SIGN_IN = 324

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observe()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                openOnline()
            } else {
                Toast.makeText(this, response?.error?.localizedMessage ?: "Error", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun observe() {
        newGameButton.setOnClickListener {
            val intent = Intent(this, NewGameActivity::class.java)
            startActivity(intent)
        }

        howToPlayButton.setOnClickListener {

            val intent = Intent(this, HowToPlayActivity::class.java)
            startActivity(intent)
        }

        tellYouFriendsButton.setOnClickListener {
            val localIntent = Intent("android.intent.action.SEND")
            localIntent.type = "text/plain"
            localIntent.putExtra(
                "android.intent.extra.TEXT",
                "Hey there, we are playing Alias!!! " + " https://play.google.com/store/apps/details?id=hr.azzi.socialgames.alias"
            )
            startActivity(Intent.createChooser(localIntent, "Share To.."))
        }

    }

    fun openOnline() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            dialog.show()
            db
                .document("Users/" + user.uid)
                .get()
                .addOnSuccessListener {
                    val username = it?.data?.get("username") as? String
                    dialog.hide()
                    if (username != null) {
                        UserManagerModel._username = username
                        goToOnline()
                    } else {
                        createUser(user)
                    }
                }
                .addOnFailureListener {
                    createUser(user)
                }
        } else {
            signIn()
        }
    }

    fun createUser(user: FirebaseUser) {

        val guestRef = db.document("Constant/guest")
        val userRef = db.document("Users/" + user.uid)

        dialog.show()
        db.runTransaction {
            val guestID= it.get(guestRef).get("guestID") as Long

            var guestUsername = "Guest$guestID"

            var userMap = hashMapOf("username" to guestUsername, "canChangeUsername" to true)
            it.set(userRef, userMap)

            val newGuestId = guestID + 1
            it.update(guestRef, "guestID", newGuestId)
            guestUsername
        }.addOnSuccessListener { username ->
            dialog.hide()
            UserManagerModel._username = username
            goToOnline()
        }.addOnFailureListener { e ->
            dialog.hide()
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
        }
    }

    fun goToOnline() {
        val intent = Intent(this, OnlineDashboardActivity::class.java)
        startActivity(intent)
    }

    fun signIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build())

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }
}
