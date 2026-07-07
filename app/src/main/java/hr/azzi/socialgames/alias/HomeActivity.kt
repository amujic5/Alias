package hr.azzi.socialgames.alias

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.appupdate.AppUpdateController
import hr.azzi.socialgames.alias.ui.screens.HomeScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class HomeActivity : BaseActivity() {

    // Registered before the activity is RESUMED so the Play update flow can report back.
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> updateController.onFlowResult(result.resultCode) }

    private val updateController: AppUpdateController by lazy {
        AppUpdateController(this, lifecycleScope, updateLauncher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JSONService.loadBoardGamesFromFile(this)
        // Gather UMP consent (GDPR/EEA) before any ad is requested; starts the Mobile
        // Ads SDK only once consent allows it. Mirrors the iOS AdsConsentManager.
        AdsConsent.gather(this)
        // Check for a required/optional update once the home screen is up
        // (Firebase is already configured; runs off the launch path).
        updateController.start()
        // NB: use setContentView(ComposeView) rather than the androidx.activity.compose
        // setContent extension. On Android 15 (API 35) that extension intermittently
        // NPEs (decorView.findViewById(android.R.id.content) is null) because AppCompat's
        // sub-decor isn't installed yet at cold start. AppCompat's setContentView installs
        // it correctly first. See BaseActivity.setComposeContent.
        setComposeContent {
            AliasTheme {
                HomeScreen(
                    onNewGame = { startActivity(Intent(this, ChooseGame::class.java)) },
                    onHowToPlay = { startActivity(Intent(this, HowToPlayActivity::class.java)) },
                    onShare = { shareApp() },
                    onAiChallenge = { startActivity(Intent(this, AIChallengeActivity::class.java)) },
                    onOnlinePlay = { startActivity(Intent(this, OnlineActivity::class.java)) },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        updateController.onResume()
    }

    override fun onDestroy() {
        updateController.dispose()
        super.onDestroy()
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

    /**
     * Sets Compose UI as the activity content via AppCompat's setContentView, which
     * installs the sub-decor (android.R.id.content) before Compose reads it. The
     * androidx.activity.compose.setContent extension skips that and intermittently
     * NPEs at cold start on Android 15 (API 35). Use this from every activity.
     */
    protected fun setComposeContent(content: @Composable () -> Unit) {
        setContentView(ComposeView(this).apply { setContent(content) })
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