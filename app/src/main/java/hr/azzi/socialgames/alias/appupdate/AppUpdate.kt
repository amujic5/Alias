package hr.azzi.socialgames.alias.appupdate

/** The three possible outcomes of an update check. */
enum class AppUpdateType { NONE, SOFT, FORCE }

/** Decides whether the app needs updating. Returns only the decision — the
 *  Play update flow / dialogs are driven elsewhere. */
interface AppVersionChecking {
    suspend fun checkForUpdate(): AppUpdateType
}

/** Static config: Remote Config keys, safe defaults, and the Play Store URL. */
object AppUpdateConfig {
    const val MIN_SUPPORTED_VERSION_KEY = "android_min_supported_version"
    const val LATEST_VERSION_KEY = "android_latest_version"

    const val DEFAULT_MIN_SUPPORTED_VERSION = "1.0.0"
    const val DEFAULT_LATEST_VERSION = "1.0.0"

    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=hr.azzi.socialgames.alias"
}
