package hr.azzi.socialgames.alias.Service

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class BlackFridayTest {

    private val zagreb = ZoneId.of("Europe/Zagreb")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(text: String, zone: ZoneId = zagreb) =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    @After fun clearForce() { BlackFriday.forcedPhase = null }

    // MARK: Calendar

    @Test fun blackFridayIsTheLastFridayOfNovember() {
        assertEquals(LocalDate.of(2026, 11, 27), BlackFriday.blackFriday(2026))
        assertEquals(LocalDate.of(2027, 11, 26), BlackFriday.blackFriday(2027))
        assertEquals(LocalDate.of(2028, 11, 24), BlackFriday.blackFriday(2028))
        // November 30th is itself a Friday.
        assertEquals(LocalDate.of(2029, 11, 30), BlackFriday.blackFriday(2029))
        assertEquals(LocalDate.of(2030, 11, 29), BlackFriday.blackFriday(2030))
    }

    @Test fun everyYearLandsOnAFridayInTheLastWeek() {
        for (year in 2026..2060) {
            val d = BlackFriday.blackFriday(year)
            assertEquals(java.time.DayOfWeek.FRIDAY, d.dayOfWeek)
            assertEquals(11, d.monthValue)
            assertTrue("$year → $d", d.dayOfMonth >= 24)
        }
    }

    @Test fun nothingOutsideTheWindows() {
        assertEquals(BlackFriday.Phase.None, BlackFriday.phase(at("2026-10-18T12:00"), zagreb))
        assertEquals(BlackFriday.Phase.None, BlackFriday.phase(at("2026-11-19T23:59:59"), zagreb))
        assertEquals(BlackFriday.Phase.None, BlackFriday.phase(at("2026-12-05T00:00"), zagreb))
        assertEquals(BlackFriday.Phase.None, BlackFriday.phase(at("2026-06-01T00:00"), zagreb))
    }

    @Test fun teaserIsTheSevenDaysBefore() {
        val opens = at("2026-11-27T00:00")
        assertEquals(BlackFriday.Phase.Teaser(opens), BlackFriday.phase(at("2026-11-20T00:00"), zagreb))
        assertEquals(BlackFriday.Phase.Teaser(opens), BlackFriday.phase(at("2026-11-26T23:59:59"), zagreb))
    }

    @Test fun saleIsFridayAndTheSevenDaysAfter() {
        val ends = at("2026-12-05T00:00")
        assertEquals(BlackFriday.Phase.Sale(ends), BlackFriday.phase(at("2026-11-27T00:00"), zagreb))
        assertEquals(BlackFriday.Phase.Sale(ends), BlackFriday.phase(at("2026-12-04T23:59:59"), zagreb))
    }

    @Test fun windowsFollowThePlayersOwnMidnight() {
        // Friday 00:30 in Zagreb is still Thursday evening in New York.
        val instant = at("2026-11-27T00:30", zagreb)
        assertTrue(BlackFriday.phase(instant, zagreb) is BlackFriday.Phase.Sale)
        assertTrue(BlackFriday.phase(instant, newYork) is BlackFriday.Phase.Teaser)
    }

    @Test fun worksInAYearWhoseBlackFridayIsTheThirtieth() {
        // 2029: the sale runs into December.
        assertTrue(BlackFriday.phase(at("2029-12-07T12:00"), zagreb) is BlackFriday.Phase.Sale)
        assertEquals(BlackFriday.Phase.None, BlackFriday.phase(at("2029-12-08T00:00"), zagreb))
        assertTrue(BlackFriday.phase(at("2029-11-23T00:00"), zagreb) is BlackFriday.Phase.Teaser)
    }

    @Test fun forcedPhaseOverridesTheCalendar() {
        val now = at("2026-06-01T12:00")
        BlackFriday.forcedPhase = "sale"
        assertTrue(BlackFriday.phase(now, zagreb) is BlackFriday.Phase.Sale)
        BlackFriday.forcedPhase = "teaser"
        assertTrue(BlackFriday.phase(now, zagreb) is BlackFriday.Phase.Teaser)
    }

    // MARK: Showing rules

    @Test fun saleShowsOnLoadThenEveryFifthReturn() {
        val shown = (1..16).filter { BlackFriday.isSaleAppearance(it) }
        assertEquals(listOf(1, 6, 11, 16), shown)
        assertFalse(BlackFriday.isSaleAppearance(0))
    }

    // MARK: Prices

    private fun eur(micros: Long) = Billing.Price("x", micros, "EUR")

    @Test fun discountIsAgainstTheSumOfEverythingElse() {
        val offers = mapOf(
            BlackFriday.PRODUCT_ID to Billing.Price("14,99 €", 14_990_000, "EUR"),
            "alias_nba" to eur(2_990_000),
            "alias_movies" to eur(2_990_000),
            "game_alias_who_am_i" to eur(2_990_000),
            "easy_medium_hard" to eur(2_990_000),
            "draw_and_guess" to eur(2_990_000),
            "after_dark" to eur(9_090_000),
            "no_ads" to eur(4_990_000),
        )
        val p = BlackFriday.pricing(offers, Locale.GERMANY)!!
        assertEquals("14,99 €", p.price)          // Play's own string, untouched
        assertEquals("29,03 €", p.fullPrice.replace(' ', ' '))
        assertEquals(48, p.discountPercent)        // 48.36… rounded down, never up
    }

    @Test fun exactHalfIsFifty() {
        val offers = mapOf(
            BlackFriday.PRODUCT_ID to Billing.Price("$5.00", 5_000_000, "USD"),
            "a" to Billing.Price("$4.00", 4_000_000, "USD"),
            "b" to Billing.Price("$6.00", 6_000_000, "USD"),
        )
        val p = BlackFriday.pricing(offers, Locale.US)!!
        assertEquals("$10.00", p.fullPrice)
        assertEquals(50, p.discountPercent)
    }

    @Test fun noPricingUntilEverythingIsKnown() {
        assertNull(BlackFriday.pricing(emptyMap()))
        // Bundle alone: nothing to compare against.
        assertNull(BlackFriday.pricing(mapOf(BlackFriday.PRODUCT_ID to eur(4_990_000))))
        // The rest without the bundle.
        assertNull(BlackFriday.pricing(mapOf("alias_nba" to eur(990_000))))
    }

    @Test fun noPricingWhenItIsNotADiscount() {
        val offers = mapOf(
            BlackFriday.PRODUCT_ID to eur(9_990_000),
            "alias_nba" to eur(990_000),
            "no_ads" to eur(4_990_000),
        )
        assertNull(BlackFriday.pricing(offers))
    }

    @Test fun ignoresPricesInAnotherCurrency() {
        val offers = mapOf(
            BlackFriday.PRODUCT_ID to eur(5_000_000),
            "a" to eur(10_000_000),
            "b" to Billing.Price("$99", 99_000_000, "USD"),
        )
        assertEquals(50, BlackFriday.pricing(offers, Locale.GERMANY)!!.discountPercent)
    }
}
