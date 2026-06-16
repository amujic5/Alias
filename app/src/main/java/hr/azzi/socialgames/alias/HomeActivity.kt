package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.ui.screens.HomeScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class HomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JSONService.loadBoardGamesFromFile(this)
        setContent {
            AliasTheme {
                HomeScreen(
                    onNewGame = { startActivity(Intent(this, ChooseGame::class.java)) },
                    onHowToPlay = { startActivity(Intent(this, HowToPlayActivity::class.java)) },
                    onShare = { shareApp() },
                    onAiChallenge = { startActivity(Intent(this, AIChallengeActivity::class.java)) },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    private fun shareApp() {
        val localIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey there, we are playing Alias!!!  https://play.google.com/store/apps/details?id=hr.azzi.socialgames.alias"
            )
        }
        startActivity(Intent.createChooser(localIntent, "Share To.."))
    }
}

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let app draw edge-to-edge; we’ll apply padding manually
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun applyInsets(target: View) {
        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding while keeping any existing left/right
            v.setPadding(v.paddingLeft, sysBars.top, v.paddingRight, sysBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }
}