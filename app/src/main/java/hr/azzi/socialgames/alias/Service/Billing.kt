package hr.azzi.socialgames.alias.Service

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing for the paid decks. Mirrors the iOS IAPStore logic:
 * a deck is unlocked when `deck.free || Billing.isUnlocked(deck.id.serializedName())`.
 *
 * Product IDs MUST match the managed (one-time) products created in the Play Console,
 * and are intentionally the same strings as the iOS product IDs.
 */
object Billing {

    /** Managed one-time product IDs — same as the paid deck ids / iOS products. */
    val productIds = listOf("alias_nba", "alias_movies", "game_alias_who_am_i")

    private val _unlocked = MutableStateFlow<Set<String>>(emptySet())
    /** Product IDs the user owns. */
    val unlocked: StateFlow<Set<String>> = _unlocked.asStateFlow()

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    /** productId -> localized formatted price (e.g. "€2.99"). */
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

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

    /** Re-query owned purchases (e.g. on resume). Safe to call anytime. */
    fun refresh() {
        if (billingClient?.isReady == true) scope.launch { queryPurchases() } else connect()
    }

    private fun connect() {
        val client = billingClient ?: return
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
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
        result.productDetailsList?.forEach { pd ->
            productDetails[pd.productId] = pd
            pd.oneTimePurchaseOfferDetails?.formattedPrice?.let { priceMap[pd.productId] = it }
        }
        if (priceMap.isNotEmpty()) _prices.value = priceMap
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
        _unlocked.value = owned
        persist()
    }

    private fun handlePurchase(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        acknowledgeIfNeeded(p)
        _unlocked.value = _unlocked.value.toMutableSet().apply { addAll(p.products) }
        persist()
    }

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
        val pd = productDetails[productId] ?: return
        val paramsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    private fun persist() {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putStringSet(KEY_UNLOCKED, _unlocked.value)?.apply()
    }

    private fun loadPersisted() {
        val stored = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()
        if (stored.isNotEmpty()) _unlocked.value = stored.toSet()
    }

    private const val PREFS = "billing"
    private const val KEY_UNLOCKED = "unlocked"
}
