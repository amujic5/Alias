package hr.azzi.socialgames.alias

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
        // Re-filter the deck list in case the language was changed in Settings —
        // country ("local-theme") decks follow the selected language.
        JSONService.loadBoardGamesFromFile(this)
        updateController.onResume()
    }

    // The activity handles the locale configChange itself (see manifest) instead of
    // being relaunched — that relaunch intermittently crashed AppCompat's sub-decor.
    // Compose recomposes with the new language automatically; we just re-filter the
    // country decks so they follow the freshly selected language.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        JSONService.loadBoardGamesFromFile(this)
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

    /**
     * Sets Compose UI as the activity content via AppCompat's setContentView, which
     * installs the sub-decor (android.R.id.content) before Compose reads it. The
     * androidx.activity.compose.setContent extension skips that and intermittently
     * NPEs at cold start on Android 15 (API 35). Use this from every activity.
     *
     * Edge-to-edge is enabled AFTER setContentView. Calling
     * WindowCompat.setDecorFitsSystemWindows() earlier (e.g. in onCreate) forces an
     * early getDecorView()/installDecor() that races AppCompat's lazy sub-decor and
     * crashes on locale-change relaunch with "Window couldn't find content container
     * view" (configChanges include locale + layoutDirection when the language switches).
     */
    protected fun setComposeContent(content: @Composable () -> Unit) {
        val composeView = ComposeView(this).apply {
            // Dispose the composition with the view-tree lifecycle so recreation
            // (e.g. a locale change) tears down cleanly.
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent(content)
        }
        setContentView(composeView)
        // Draw edge-to-edge; Compose applies the system-bar padding itself.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun applyInsets(target: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding while keeping any existing left/right
            v.setPadding(v.paddingLeft, sysBars.top, v.paddingRight, sysBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }
}