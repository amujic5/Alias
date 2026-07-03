package hr.azzi.socialgames.alias.appupdate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.AppUpdateType as PlayUpdateType
import hr.azzi.socialgames.alias.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Runs the version check once and drives the update UX with Google Play In-App
 * Updates:
 *
 * - FORCE → Play **immediate** update (full-screen, blocking). Falls back to a
 *   non-cancelable dialog linking to the Play Store if an in-app update isn't
 *   available.
 * - SOFT  → Play **flexible** update (background download), throttled to every
 *   3rd detection so it doesn't nag on every launch. Falls back to a custom soft
 *   dialog if a flexible update isn't available.
 *
 * The version decision comes from [AppVersionChecking]; this class only turns it
 * into UI, so the two concerns stay separate.
 */
class AppUpdateController(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val checker: AppVersionChecking = FirebaseAppVersionChecker(),
) {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var flexibleListener: InstallStateUpdatedListener? = null
    private var lastType: AppUpdateType = AppUpdateType.NONE

    fun start() {
        scope.launch {
            lastType = checker.checkForUpdate()
            when (lastType) {
                AppUpdateType.NONE -> Unit
                AppUpdateType.FORCE -> beginForce()
                AppUpdateType.SOFT -> if (shouldShowSoft()) beginSoft()
            }
        }
    }

    /** Resume an interrupted immediate update, or offer to install a finished
     *  flexible download. Call from the host Activity's `onResume`. */
    fun onResume() {
        scope.launch {
            val info = info() ?: return@launch
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startFlow(info, PlayUpdateType.IMMEDIATE)
            } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                promptCompleteFlexible()
            }
        }
    }

    /** Forward the update-flow result. If a required update was dismissed, keep pushing. */
    fun onFlowResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK && lastType == AppUpdateType.FORCE) {
            scope.launch { beginForce() }
        }
    }

    fun dispose() {
        flexibleListener?.let { manager.unregisterListener(it) }
        flexibleListener = null
    }

    // MARK: flows

    private suspend fun beginForce() {
        val info = info()
        if (info != null &&
            info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(PlayUpdateType.IMMEDIATE)
        ) {
            startFlow(info, PlayUpdateType.IMMEDIATE)
        } else {
            showForceDialog()
        }
    }

    private suspend fun beginSoft() {
        val info = info()
        if (info != null &&
            info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(PlayUpdateType.FLEXIBLE)
        ) {
            registerFlexibleListener()
            startFlow(info, PlayUpdateType.FLEXIBLE)
        } else {
            showSoftDialog()
        }
    }

    private suspend fun info(): AppUpdateInfo? =
        runCatching { manager.appUpdateInfo.await() }.getOrNull()

    private fun startFlow(info: AppUpdateInfo, type: Int) {
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }
    }

    private fun registerFlexibleListener() {
        if (flexibleListener != null) return
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) promptCompleteFlexible()
        }
        flexibleListener = listener
        manager.registerListener(listener)
    }

    // MARK: dialogs (fallbacks + flexible completion)

    private fun promptCompleteFlexible() {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_flexible_downloaded_title)
            .setMessage(R.string.update_flexible_downloaded_msg)
            .setPositiveButton(R.string.update_action_restart) { _, _ -> manager.completeUpdate() }
            .setNegativeButton(R.string.update_action_later, null)
            .show()
    }

    private fun showForceDialog() {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle(R.string.update_force_title)
            .setMessage(R.string.update_force_message)
            .setPositiveButton(R.string.update_action_update) { _, _ -> openPlayStore() }
            .show()
    }

    private fun showSoftDialog() {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_soft_title)
            .setMessage(R.string.update_soft_message)
            .setPositiveButton(R.string.update_action_update) { _, _ -> openPlayStore() }
            .setNegativeButton(R.string.update_action_later, null)
            .show()
    }

    private fun openPlayStore() {
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateConfig.PLAY_STORE_URL)))
        }
    }

    /** Show the soft prompt on every 3rd detection (1st, 4th, 7th…). */
    private fun shouldShowSoft(): Boolean {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(SOFT_COUNTER_KEY, 0) + 1
        prefs.edit().putInt(SOFT_COUNTER_KEY, count).apply()
        return count % SOFT_FREQUENCY == 1
    }

    companion object {
        private const val PREFS = "app_update"
        private const val SOFT_COUNTER_KEY = "soft_prompt_counter"
        private const val SOFT_FREQUENCY = 3
    }
}
