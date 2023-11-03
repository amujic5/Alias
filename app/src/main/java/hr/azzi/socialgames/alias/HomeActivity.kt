package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.loadingview.LoadingDialog
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.databinding.ActivityHomeBinding

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding : ActivityHomeBinding

    val dialog: LoadingDialog by lazy {
        LoadingDialog.get(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)

        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        JSONService.loadBoardGamesFromFile(this)
        observe()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    fun observe() {
        binding.newGameButton.setOnClickListener {
            val intent = Intent(this, ChooseGame::class.java)
            startActivity(intent)
        }

        binding.howToPlayButton.setOnClickListener {

            val intent = Intent(this, HowToPlayActivity::class.java)
            startActivity(intent)
        }

        binding.tellYouFriendsButton.setOnClickListener {
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
