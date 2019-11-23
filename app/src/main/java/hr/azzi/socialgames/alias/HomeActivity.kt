package hr.azzi.socialgames.alias

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.FirebaseDatabase
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.synthetic.main.activity_home.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class HomeActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observe()

        val a = DictionaryService()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
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
}
