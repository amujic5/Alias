package hr.azzi.socialgames.alias.Service

/**
 * Whether the player has bought their way out of the ads.
 *
 * What the entitlement removes is the interstitials — the ads nobody asked for. The rewarded
 * videos are a trade the player opts into for a deck, and they stay: taking those away would
 * take the free unlock with them.
 *
 * Backed by [Billing], which loads what is owned from preferences before it has heard back
 * from Play, so the answer is available synchronously from the moment the app starts — which
 * is what the callers need, since an interstitial decides whether to preload long before any
 * screen is on show.
 */
object NoAds {

    const val PRODUCT_ID = "no_ads"

    val isPurchased: Boolean get() = Billing.isUnlocked(PRODUCT_ID)

    /** Localized, from Play — never a price typed into the app. */
    val formattedPrice: String? get() = Billing.priceOf(PRODUCT_ID)
}
