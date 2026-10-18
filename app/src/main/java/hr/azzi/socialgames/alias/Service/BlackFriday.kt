package hr.azzi.socialgames.alias.Service

import android.content.Context
import androidx.core.content.edit
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Currency
import java.util.Locale

/**
 * The Black Friday bundle — one purchase that unlocks everything.
 *
 * Android port of iOS `BlackFriday.swift`.
 *
 * Recurs every year without a release: the dates are worked out from the calendar, not
 * written down. Black Friday is taken as the last Friday of November, and around it there
 * are two windows:
 *
 * - **Teaser**, the seven days before it. The offer is announced but cannot be bought; the
 *   full screen goes up once per year and after that it lives on as a chip on Home.
 * - **Sale**, Black Friday itself and the seven days after. The full screen goes up when
 *   Home is created and on every fifth return to it, until the player buys it or the week
 *   runs out.
 *
 * Outside the windows nothing is shown. The product stays active in the Play Console all
 * year — the app simply does not offer it — so a restore works in any month.
 */
object BlackFriday {

    const val PRODUCT_ID = "black_friday_full_bundle"

    /** Days of teaser before Black Friday, and days of sale after it. */
    const val TEASER_DAYS = 7L
    const val SALE_DAYS_AFTER = 7L

    /** Home shows the sale when it is created, then every this-many returns to it. */
    const val SALE_EVERY_NTH_APPEARANCE = 5

    sealed interface Phase {
        data object None : Phase
        /** Announced; buying opens at [opensAt]. */
        data class Teaser(val opensAt: Instant) : Phase
        /** On sale until [endsAt]. */
        data class Sale(val endsAt: Instant) : Phase
    }

    /**
     * Debug builds only: `adb shell am start … --es bfForcePhase sale` (or `teaser`) to see
     * either window outside November. Set by HomeActivity; ignored in release.
     */
    var forcedPhase: String? = null

    // MARK: Ownership

    /** Whether the bundle was bought. [Billing] expands it into every other product. */
    val isOwned: Boolean get() = Billing.isUnlocked(PRODUCT_ID)

    // MARK: Calendar

    /** The last Friday of November in [year]. */
    fun blackFriday(year: Int): LocalDate =
        LocalDate.of(year, 11, 30).with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))

    /**
     * Which window [now] falls in, in the player's own time zone — so the sale opens at
     * their midnight rather than somebody else's.
     */
    fun phase(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Phase {
        when (forcedPhase) {
            "teaser" -> return Phase.Teaser(now.plusSeconds(2 * 86_400L))
            "sale" -> return Phase.Sale(now.plusSeconds(86_400L))
        }
        val year = now.atZone(zone).year
        val friday = blackFriday(year)
        val teaserStart = friday.minusDays(TEASER_DAYS).atStartOfDay(zone).toInstant()
        val opens = friday.atStartOfDay(zone).toInstant()
        // The end of the seventh day after, i.e. midnight starting the eighth.
        val saleEnd = friday.plusDays(SALE_DAYS_AFTER + 1).atStartOfDay(zone).toInstant()
        return when {
            now >= teaserStart && now < opens -> Phase.Teaser(opens)
            now >= opens && now < saleEnd -> Phase.Sale(saleEnd)
            else -> Phase.None
        }
    }

    /** Whether there is anything to show at all — a window is open and it is not owned. */
    fun isOffered(now: Instant = Instant.now()): Boolean = !isOwned && phase(now) != Phase.None

    // MARK: Prices

    /** The bundle's price, and what everything it replaces would cost bought one by one. */
    data class Pricing(val price: String, val fullPrice: String, val discountPercent: Int)

    /**
     * The struck-through number is the sum of every other product Play sells, in the
     * bundle's currency, and the discount follows from the two. Nothing is typed in —
     * change a price in the Play Console and all three follow it.
     *
     * `null` until Play has priced the bundle and the rest, or when the sum would not be
     * a discount at all — an offer that cannot quote its saving does not quote one.
     *
     * @param offers productId → price, as Play returned them.
     */
    fun pricing(offers: Map<String, Billing.Price>, locale: Locale = Locale.getDefault()): Pricing? {
        val bundle = offers[PRODUCT_ID] ?: return null
        val others = offers.filterKeys { it != PRODUCT_ID }.values
            .filter { it.currencyCode == bundle.currencyCode }
        val totalMicros = others.sumOf { it.amountMicros }
        if (totalMicros <= 0 || totalMicros <= bundle.amountMicros) return null

        val discount = BigDecimal.ONE
            .minus(BigDecimal(bundle.amountMicros).divide(BigDecimal(totalMicros), 6, RoundingMode.HALF_UP))
            .movePointRight(2)
            .setScale(0, RoundingMode.DOWN)   // never claim more than the truth
            .toInt()

        val format = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(bundle.currencyCode)
        }
        return Pricing(
            price = bundle.formatted,
            fullPrice = format.format(BigDecimal(totalMicros).movePointLeft(6)),
            discountPercent = discount,
        )
    }

    // MARK: Showing rules

    /**
     * Whether the sale goes up on this appearance of Home: on the first, then on the 6th,
     * 11th… — every fifth return.
     */
    fun isSaleAppearance(appearance: Int): Boolean =
        appearance == 1 || (appearance > 1 && (appearance - 1) % SALE_EVERY_NTH_APPEARANCE == 0)

    /**
     * Whether the full screen should go up on this appearance of Home.
     *
     * @param appearance 1 for the first appearance after Home was created, counting up.
     */
    fun isDue(context: Context, appearance: Int, now: Instant = Instant.now()): Boolean {
        if (isOwned) return false
        return when (val phase = phase(now)) {
            Phase.None -> false
            // Once per year. The chip on Home is how it is reopened after that.
            is Phase.Teaser -> !prefs(context).getBoolean(teaserKey(phase), false)
            is Phase.Sale -> isSaleAppearance(appearance)
        }
    }

    /** Counted once it is on screen, not when it is asked for. */
    fun recordShown(context: Context, now: Instant = Instant.now()) {
        val phase = phase(now)
        if (phase is Phase.Teaser) prefs(context).edit { putBoolean(teaserKey(phase), true) }
    }

    private fun teaserKey(phase: Phase.Teaser): String =
        "teaser_shown_${phase.opensAt.atZone(ZoneId.systemDefault()).year}"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("black_friday", Context.MODE_PRIVATE)
}
