package hr.azzi.socialgames.alias.Service

/**
 * The audience a finished replay could be shown to.
 *
 * There is one account today — the Balkan TikTok — and every other language has none. The
 * type exists so that the second account is a row in [ALL] rather than another `if` spread
 * through the results screen: the card, the handle shown to the player and the folder the
 * upload lands in all read from here.
 *
 * Matching is by *deck* language, not by the app's interface language. Somebody with the
 * app in English who picks the Croatian deck has recorded a Croatian round, and that is
 * the audience it belongs to.
 */
data class SocialRegion(
    /**
     * Stable identifier. It becomes a folder in Storage, so renaming it re-homes every
     * upload that comes after — treat it as permanent once uploads exist.
     */
    val id: String,
    /** Deck language codes this account speaks, spelled as the dictionaries spell them. */
    val deckLanguageCodes: Set<String>,
    /** Shown under "Follow us on TikTok" on the card. */
    val tiktokHandle: String,
    val tiktokUrl: String,
) {
    companion object {
        val BALKAN = SocialRegion(
            id = "balkan",
            deckLanguageCodes = setOf("CRO", "BOS", "SRB", "SLO"),
            tiktokHandle = "@aliaswordsbalkan",
            tiktokUrl = "https://www.tiktok.com/@aliaswordsbalkan",
        )

        /** Every account that exists. Adding one is this line plus its constant above. */
        val ALL = listOf(BALKAN)

        /**
         * The account for a deck language, or null where that language has none.
         *
         * Null is the common case, so callers must handle it rather than fall back to the
         * Balkan account — which would offer a Croatian audience to somebody who played in
         * German.
         */
        fun forDeckLanguage(code: String?): SocialRegion? {
            val wanted = code?.uppercase() ?: return null
            return ALL.firstOrNull { wanted in it.deckLanguageCodes }
        }
    }
}
