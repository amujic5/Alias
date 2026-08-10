package hr.azzi.socialgames.alias.Service

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** One in-app UI language (each ships a `values-<code>` resource folder). */
data class AppLanguage(val code: String, val nativeName: String, val flag: String)

/**
 * In-app UI language switching, ported from the iOS LanguageManager. Uses the
 * AppCompat per-app locale API (`setApplicationLocales`) so switching changes the
 * whole UI immediately and is persisted by the system (see the AppLocales service
 * declared in the manifest for pre-API-33 storage).
 *
 * The chosen language also drives the deck language (deckLanguageCode), the Home
 * background image and which local-theme "country" deck is shown.
 */
object LanguageManager {

    /** The languages the app ships a `values-<code>` folder for. */
    val available: List<AppLanguage> = listOf(
        AppLanguage("en", "English", "🇬🇧"),
        AppLanguage("hr", "Hrvatski", "🇭🇷"),
        AppLanguage("bs", "Bosanski", "🇧🇦"),
        AppLanguage("sr", "Srpski", "🇷🇸"),
        AppLanguage("sl", "Slovenščina", "🇸🇮"),
        AppLanguage("de", "Deutsch", "🇩🇪"),
        AppLanguage("es", "Español", "🇪🇸"),
        AppLanguage("fr", "Français", "🇫🇷"),
        AppLanguage("it", "Italiano", "🇮🇹"),
    )

    /** Currently selected UI language code — the app locale, else best device match. */
    val currentCode: String
        get() {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (!locales.isEmpty) {
                val tag = locales[0]?.language?.lowercase()
                available.firstOrNull { it.code == tag }?.let { return it.code }
            }
            return regionPreferredCode() ?: devicePreferredCode() ?: "en"
        }

    val current: AppLanguage
        get() = available.firstOrNull { it.code == currentCode } ?: available[0]

    /** Persists a new language and rebuilds the UI so it takes effect app-wide. */
    fun setLanguage(code: String) {
        if (available.none { it.code == code }) return
        // Serbian is always Latin (latinica) in this app — apply the explicit script tag
        // so the system never falls back to Cyrillic. currentCode still reports "sr".
        val tag = if (code == "sr") "sr-Latn" else code
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Bundled-deck language short-code matching the current UI language (games.json
     *  / dictionary `languageCode`, e.g. "CRO", "SRB", "BOS"). Preselects the deck
     *  language across every game mode and gates the local-theme country decks. */
    val deckLanguageCode: String
        get() = when (currentCode) {
            "en" -> "ENG"; "hr" -> "CRO"; "bs" -> "BOS"; "sr" -> "SRB"
            "sl" -> "SLO"; "de" -> "GER"; "es" -> "ESP"; "fr" -> "FRA"; "it" -> "ITA"
            else -> "ENG"
        }

    /** Home background drawable name for the current language (empty = default). */
    val homeImageName: String
        get() = when (currentCode) {
            "bs" -> "home_bosnia"; "hr" -> "home_croatia"; "sr" -> "home_serbia"
            "de" -> "home_germany"; "sl" -> "home_slovenia"; "en" -> "home_uk"
            "es" -> "home_spain"; "it" -> "home_italy"; "fr" -> "home_france"
            else -> ""
        }

    // MARK: - Device fallbacks (region first, then language)

    private fun regionPreferredCode(): String? {
        val code = when (Locale.getDefault().country.uppercase()) {
            "GB", "US", "IE", "AU", "CA", "NZ" -> "en"
            "HR" -> "hr"; "BA" -> "bs"; "RS", "ME" -> "sr"; "SI" -> "sl"
            "DE", "AT", "CH", "LI" -> "de"; "ES" -> "es"; "FR", "LU", "MC" -> "fr"
            "IT", "SM", "VA" -> "it"
            else -> null
        }
        return code?.takeIf { c -> available.any { it.code == c } }
    }

    private fun devicePreferredCode(): String? {
        val lang = Locale.getDefault().language.lowercase()
        return available.firstOrNull { it.code == lang }?.code
    }
}
