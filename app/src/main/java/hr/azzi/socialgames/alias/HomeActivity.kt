package hr.azzi.socialgames.alias

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
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
import hr.azzi.socialgames.alias.Service.Analytics
import hr.azzi.socialgames.alias.Service.Billing
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
        // A purchase made elsewhere — another device, or the Play store itself — should be
        // honoured here without waiting for a cold start.
        Billing.refresh()
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
        try {
            setContentView(composeView)
        } catch (error: RuntimeException) {
            if (!restartOnBrokenDecor(error)) throw error
            return
        }
        // Draw edge-to-edge; Compose applies the system-bar padding itself.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /**
     * Last resort for a window that comes up without its content container.
     *
     * AppCompat builds its sub-decor by handing the framework's `android.R.id.content`
     * view over to its own ContentFrameLayout. When the framework hands it nothing —
     * `mWindow.findViewById(android.R.id.content)` is null — the handover is skipped and
     * the install dies moments later, either on `applyFixedSizeWindow` (NPE on
     * ContentFrameLayout.setDecorPadding) or, earlier, on "Window couldn't find content
     * container view". It happens at cold start on a small number of devices and is not
     * something the app can prevent from here.
     *
     * The recovery has to be a fresh activity, not a second `setContentView`: the failed
     * install already made the sub-decor this window's content, and that sub-decor is the
     * one whose id was never set, so retrying in place meets the same null. A new instance
     * gets a new Window.
     *
     * Once per activity class per process. If the restarted instance fails the same way
     * the cause is not transient, and the crash is let through rather than restarting for
     * as long as the user's patience holds.
     */
    private fun restartOnBrokenDecor(error: RuntimeException): Boolean {
        if (!restartedOnce.add(javaClass.name)) return false
        Log.w("Decor", "sub-decor install failed, restarting ${javaClass.simpleName}", error)
        Analytics.log(this, Analytics.DECOR_RESTART)
        finish()
        startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
        return true
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

    private companion object {
        /** Activity classes already restarted once — see [restartOnBrokenDecor]. */
        val restartedOnce = mutableSetOf<String>()
    }
}