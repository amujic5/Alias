package hr.azzi.socialgames.alias.Service

import android.content.Context

/**
 * Which decks have already spent their one free try.
 *
 * A free try that can be taken again is not a try — it is the deck being free with extra
 * steps — so the record has to outlast the session it was earned in. It lives in its own
 * SharedPreferences file rather than alongside the billing state: this is not an
 * entitlement, and mixing the two would mean a restore-purchases sweep could plausibly
 * clear it.
 *
 * Reinstalling generally does not hand out a second round either, because Android's
 * auto-backup restores app preferences with the app (`allowBackup` is on in the
 * manifest). That is weaker than the iOS keychain, which survives deletion outright, but
 * it is the closest equivalent the platform offers without a server.
 *
 * Keyed by the product a deck would unlock, so the record survives a deck being renamed.
 */
object FreeTryStore {

    private const val PREFS = "free_try"
    private const val KEY = "spent"

    fun hasUsedFreeTry(context: Context, productId: String): Boolean =
        spent(context).contains(productId)

    fun markFreeTryUsed(context: Context, productId: String) {
        val updated = spent(context) + productId
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            // A fresh set: SharedPreferences hands back the live instance from its cache,
            // and mutating that one in place is the classic way to have the change not
            // stick until the next process.
            .putStringSet(KEY, HashSet(updated))
            .apply()
    }

    private fun spent(context: Context): Set<String> =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()
}
