package hr.azzi.socialgames.alias

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.synthetic.main.activity_home.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class HomeActivity : AppCompatActivity() {


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

        onlineButton.setOnClickListener {
            openOnline()
        }

    }

    fun openOnline() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val intent = Intent(this, OnlineDashboardActivity::class.java)
            startActivity(intent)
        } else {
            signIn()
        }

    }

    fun signIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build())

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }
}
