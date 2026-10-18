package hr.azzi.socialgames.alias.Service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import hr.azzi.socialgames.alias.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing for the paid decks. Mirrors the iOS IAPStore logic:
 * a deck is unlocked when `deck.free || Billing.isUnlocked(deck.productId)`. Not the deck
 * id: Draw & Guess is sold as `draw_and_guess` while the deck is `game_draw_guess`.
 *
 * Product IDs MUST match the managed (one-time) products created in the Play Console,
 * and are intentionally the same strings as the iOS product IDs.
 */
object Billing {

    private const val TAG = "Billing"

    /** Managed one-time product IDs — same as the paid deck ids / iOS products. */
    val productIds = listOf(
        "alias_nba", "alias_movies", "game_alias_who_am_i", "easy_medium_hard",
        // Draw & Guess. Deliberately not "game_draw_guess" — see BoardGame.productId.
        "draw_and_guess",
        // After Dark. Deliberately not "game_alias_after_dark" — see BoardGame.productId.
        "after_dark",
        NoAds.PRODUCT_ID,
        BlackFriday.PRODUCT_ID,
    )

    /**
     * Products a debug build treats as owned, so a deck can be played before its product is
     * live in the console. Empty in a release build, and never written to disk (see
     * [persist]): debug and release share an application ID, so a persisted freebie would
     * follow the user into the real app.
     *
     * Easy Medium Hard is deliberately left out — it is being tested through the real
     * checkout, and a deck that unlocks itself can't show whether that works. So is the
     * Black Friday bundle: owning it owns everything, and a debug build that owned it would
     * never see the offer.
     */
    private val devUnlocked: Set<String> =
        if (BuildConfig.DEBUG) productIds.toSet() - "easy_medium_hard" - BlackFriday.PRODUCT_ID
        else emptySet()

    private val _unlocked = MutableStateFlow(devUnlocked)
    /** Product IDs the user owns. */
    val unlocked: StateFlow<Set<String>> = _unlocked.asStateFlow()

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    /** productId -> localized formatted price (e.g. "€2.99"). */
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    /** A price as Play quotes it: the string to show, and the number behind it. */
    data class Price(val formatted: String, val amountMicros: Long, val currencyCode: String)

    private val _offers = MutableStateFlow<Map<String, Price>>(emptyMap())
    /** productId -> price with its amount, for sums like the Black Friday saving. */
    val offers: StateFlow<Map<String, Price>> = _offers.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val productDetails = mutableMapOf<String, ProductDetails>()
    private var billingClient: BillingClient? = null
    private var appContext: Context? = null

    fun isUnlocked(productId: String): Boolean = _unlocked.value.contains(productId)
    fun priceOf(productId: String): String? = _prices.value[productId]

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    /** Call once from Application.onCreate. Idempotent. */
    fun start(context: Context) {
        appContext = context.applicationContext
        loadPersisted()
        if (billingClient?.isReady == true) return
        val client = BillingClient.newBuilder(appContext!!)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        billingClient = client
        connect()
    }

    /**
     * Re-query prices and owned purchases (e.g. on resume). Safe to call anytime.
     *
     * Products as well as purchases: a product that wasn't live yet when the app connected
     * would otherwise stay priceless until the next launch.
     */
    fun refresh() {
        if (billingClient?.isReady == true) scope.launch { queryProducts(); queryPurchases() } else connect()
    }

    private fun connect() {
        val client = billingClient ?: return
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "billing setup failed: ${result.responseCode} ${result.debugMessage}")
                }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProducts()
                        queryPurchases()
                    }
                }
            }
            override fun onBillingServiceDisconnected() { /* reconnected lazily via refresh()/launchPurchase */ }
        })
    }

    private suspend fun queryProducts() {
        val client = billingClient ?: return
        val products = productIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        val result = client.queryProductDetails(params)
        val priceMap = mutableMapOf<String, String>()
        val offerMap = mutableMapOf<String, Price>()
        result.productDetailsList?.forEach { pd ->
            productDetails[pd.productId] = pd
            pd.oneTimePurchaseOfferDetails?.let { offer ->
                priceMap[pd.productId] = offer.formattedPrice
                offerMap[pd.productId] =
                    Price(offer.formattedPrice, offer.priceAmountMicros, offer.priceCurrencyCode)
            }
        }
        // A product Play doesn't return is the whole reason a deck shows no price: it is
        // still a draft, priceless in this country, or the build isn't one Play knows.
        val missing = productIds - priceMap.keys
        Log.i(
            TAG,
            "product details: code=${result.billingResult.responseCode} " +
                "${result.billingResult.debugMessage} priced=${priceMap.keys} missing=$missing",
        )
        if (priceMap.isNotEmpty()) _prices.value = priceMap
        if (offerMap.isNotEmpty()) _offers.value = offerMap
    }

    private suspend fun queryPurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        val owned = _unlocked.value.toMutableSet()
        result.purchasesList.forEach { p ->
            if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                owned.addAll(p.products)
                acknowledgeIfNeeded(p)
            }
        }
        _unlocked.value = expandingBundle(owned)
        persist()
    }

    private fun handlePurchase(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        acknowledgeIfNeeded(p)
        _unlocked.value = expandingBundle(_unlocked.value + p.products)
        persist()
    }

    /**
     * The Black Friday bundle stands in for every other product.
     *
     * Expanded here, once, rather than taught to every padlock: each deck already asks
     * [isUnlocked] and No Ads reads the same set, so a bundle owner finds all of them
     * answering yes and nothing is left on sale.
     */
    private fun expandingBundle(ids: Set<String>): Set<String> =
        if (BlackFriday.PRODUCT_ID in ids) ids + productIds else ids

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
        billingClient?.acknowledgePurchase(params) { /* best-effort; re-checked on next queryPurchases */ }
    }

    /** Launch the Google Play purchase sheet for a product. Must run on an Activity. */
    fun launchPurchase(activity: Activity, productId: String) {
        val client = billingClient
        if (client == null || !client.isReady) {
            connect()
            return
        }
        val pd = productDetails[productId]
        if (pd == null) {
            // The catalogue is fetched once at launch and a tap can beat it there — on a slow
            // network, or on the first run. Without this the button would simply do nothing,
            // which is indistinguishable from a broken one.
            scope.launch {
                queryProducts()
                productDetails[productId]?.let { launchFlow(activity, it) }
            }
            return
        }
        launchFlow(activity, pd)
    }

    private fun launchFlow(activity: Activity, product: ProductDetails) {
        val client = billingClient ?: return
        val paramsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    private fun persist() {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(KEY_UNLOCKED, _unlocked.value - devUnlocked)?.apply()
    }

    private fun loadPersisted() {
        val stored = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()
        if (stored.isNotEmpty()) _unlocked.value = expandingBundle(stored.toSet() + devUnlocked)
    }

    private const val PREFS = "billing"
    private const val KEY_UNLOCKED = "unlocked"
}
