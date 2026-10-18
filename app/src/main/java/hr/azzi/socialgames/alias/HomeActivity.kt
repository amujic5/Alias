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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import hr.azzi.socialgames.alias.Service.Analytics
import hr.azzi.socialgames.alias.Service.Billing
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.appupdate.AppUpdateController
import hr.azzi.socialgames.alias.ui.screens.HomeScreen
import hr.azzi.socialgames.alias.ui.theme.AliasTheme

class HomeActivity : BaseActivity() {

    /** Times Home has been shown since it was created — see `BlackFriday.isDue`. */
    private val appearances = androidx.compose.runtime.mutableIntStateOf(0)

    // Registered before the activity is RESUMED so the Play update flow can report back.
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> updateController.onFlowResult(result.resultCode) }

    private val updateController: AppUpdateController by lazy {
        AppUpdateController(this, lifecycleScope, updateLauncher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            hr.azzi.socialgames.alias.Service.BlackFriday.forcedPhase = intent?.getStringExtra("bfForcePhase")
        }
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
                    appearance = appearances.intValue.coerceAtLeast(1),
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
        appearances.intValue += 1
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
            recoverFromBrokenWindow(error)
            return
        }
        // Draw edge-to-edge; Compose applies the system-bar padding itself.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /**
     * Applies the recovery theme when this launch is the second attempt — see
     * [recoverFromBrokenWindow]. Has to happen before the content is set, because the
     * framework reads the window style when it builds the decor.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent?.getBooleanExtra(EXTRA_RECOVERY, false) == true) setTheme(R.style.RecoveryTheme)
        super.onCreate(savedInstanceState)
    }

    /**
     * Last resort for a window that comes up without its content container.
     *
     * The framework inflates its own screen layout and then cannot find
     * `@android:id/content` inside it — `PhoneWindow.generateLayout` throws "Window
     * couldn't find content container view". Nothing in this app takes part in that
     * inflation: no LayoutInflater factory, no getResources override, no base-context
     * wrapping. It is the device's own framework resources, at cold start.
     *
     * The throw leaves the window half-installed — a decor with no content parent — so
     * anything that touches it afterwards finds `android.R.id.content` null. That is the
     * same null AppCompat dies on in `applyFixedSizeWindow`, and the one Stream's
     * permission watcher used to die on: one fault, several places to fall over.
     *
     * Two attempts, and they are not the same attempt twice. A window cannot be repaired
     * in place — this one keeps its missing content parent for as long as it lives — so
     * the retry is a fresh activity, and it asks for a different framework layout:
     * `android:windowActionModeOverlay` makes `generateLayout` inflate
     * screen_simple_overlay_action_mode instead of screen_simple. If the device broke one
     * layout the second one works; if it broke the lot, nothing here can help.
     *
     * The second failure closes the screen quietly rather than crashing. The crash would
     * tell the user nothing — there is no window to say it in — and it would count against
     * the app in Play's vitals for a fault in the device's system resources. It is
     * recorded to Crashlytics either way, which does not count towards vitals, so the
     * devices this happens on stay visible.
     */
    private fun recoverFromBrokenWindow(error: RuntimeException) {
        val secondAttempt = intent?.getBooleanExtra(EXTRA_RECOVERY, false) == true
        Log.w(TAG, "window has no content view in ${javaClass.simpleName}", error)
        Analytics.log(this, if (secondAttempt) Analytics.DECOR_GAVE_UP else Analytics.DECOR_RESTART)
        runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
        finish()
        if (!secondAttempt) {
            startActivity(
                Intent(intent)
                    .putExtra(EXTRA_RECOVERY, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
            )
        }
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
        const val TAG = "Decor"

        /** Marks a launch as the retry, so a second failure stops instead of looping.
         *  Carried in the intent rather than in a field: it survives the new process a
         *  restart may come up in, and cannot outlive the launch it belongs to. */
        const val EXTRA_RECOVERY = "recoverBrokenWindow"
    }
}