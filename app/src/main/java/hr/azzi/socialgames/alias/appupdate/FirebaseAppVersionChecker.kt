package hr.azzi.socialgames.alias.appupdate

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import hr.azzi.socialgames.alias.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * Decides whether the app needs a soft/forced update by comparing the running
 * version against Firebase Remote Config.
 *
 * ```
 * current < android_min_supported_version → FORCE
 * current < android_latest_version        → SOFT
 * otherwise                               → NONE
 * ```
 *
 * A fetch failure never blocks the user — it falls back to the last activated
 * values, or the in-code defaults on a cold start.
 */
class FirebaseAppVersionChecker(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val currentVersion: String = BuildConfig.VERSION_NAME,
) : AppVersionChecking {

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            }
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                AppUpdateConfig.MIN_SUPPORTED_VERSION_KEY to AppUpdateConfig.DEFAULT_MIN_SUPPORTED_VERSION,
                AppUpdateConfig.LATEST_VERSION_KEY to AppUpdateConfig.DEFAULT_LATEST_VERSION,
            )
        )
    }

    override suspend fun checkForUpdate(): AppUpdateType {
        // Best-effort fetch; ignore failures and use defaults / last activated values.
        runCatching { remoteConfig.fetchAndActivate().await() }

        val current = AppVersion(currentVersion)
        val minSupported = AppVersion(remoteConfig.getString(AppUpdateConfig.MIN_SUPPORTED_VERSION_KEY))
        val latest = AppVersion(remoteConfig.getString(AppUpdateConfig.LATEST_VERSION_KEY))

        return when {
            current < minSupported -> AppUpdateType.FORCE
            current < latest -> AppUpdateType.SOFT
            else -> AppUpdateType.NONE
        }
    }
}
