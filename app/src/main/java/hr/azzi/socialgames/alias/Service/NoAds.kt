package hr.azzi.socialgames.alias.Service

import hr.azzi.socialgames.alias.arena.LevelRewards
import hr.azzi.socialgames.alias.arena.PlayerLevel

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

    /** Whether it was **bought**. Written by [Billing] and by nothing else. */
    val isPurchased: Boolean get() = Billing.isUnlocked(PRODUCT_ID)

    /**
     * Whether the ads are off, however that happened.
     *
     * Two ways in: the purchase, and level 50 on the Arena ladder. [isPurchased] stays
     * the narrower question because the store still needs to know whether there is
     * anything left to sell.
     *
     * The level half is derived, never stored, so it cannot be granted by editing a
     * preference and cannot survive an account that did not earn it.
     */
    val isActive: Boolean
        get() = isPurchased || PlayerLevel.current >= LevelRewards.NO_ADS_LEVEL

    /**
     * Posted when the entitlement changes — including when a level crosses fifty.
     *
     * [isActive] is derived, so it is already true by then, but a banner laid out a
     * second ago does not re-ask.
     */
    private val listeners = mutableListOf<() -> Unit>()
    fun addListener(block: () -> Unit) { listeners += block }
    fun removeListener(block: () -> Unit) { listeners -= block }
    fun notifyChanged() = listeners.toList().forEach { it() }

    /** Localized, from Play — never a price typed into the app. */
    val formattedPrice: String? get() = Billing.priceOf(PRODUCT_ID)
}
