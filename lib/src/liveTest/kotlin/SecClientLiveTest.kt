package com.alexkouzel.sec

import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Every source answers and parses; exact values are checked offline against fixtures.
class SecClientLiveTest {
    @Test
    fun `companies`() {
        val companies = sec.companies.all()

        assertTrue(companies.size > 5_000, "only ${companies.size}")
        assertTrue(companies.any { it.ticker == "AAPL" })
    }

    @Test
    fun `filings of a company`() {
        assertValid(sec.filings.ofCompany(320193))
    }

    @Test
    fun `latest filings`() {
        assertValid(sec.filings.latest())
    }

    @Test
    fun `daily filings`() {
        assertValid(sec.filings.daily(daysAgo = 1))
    }

    @Test
    fun `previous quarter's index`() {
        val quarterAgo = LocalDate.now().minusMonths(3)

        val refs = sec.filings.quarter(quarterAgo.year, (quarterAgo.monthValue - 1) / 3 + 1)

        assertTrue(refs.size > 10_000, "only ${refs.size}")
        assertValid(refs.take(100))
    }

    @Test
    fun `a recent Form 4`() {
        // The daily list has hundreds of Form 4s; the latest feed often has none.
        val ref = sec.filings.daily(daysAgo = 1, form = FormType.F4).firstOrNull()
        assumeTrue(ref != null, "no Form 4 filed one day ago")

        val filing = sec.ownership.get(requireNotNull(ref))

        assertEquals(FormType.F4, filing.formType)
        assertTrue(filing.issuer.name != null)
        assertTrue(filing.owners.isNotEmpty())
    }

    private fun assertValid(refs: List<FilingRef>) {
        assertTrue(refs.isNotEmpty())
        refs.forEach {
            assertEquals(20, it.accessionNumber.length, it.accessionNumber)
            assertTrue(it.cik > 0)
        }
    }

    companion object {
        private val sec = liveClient()

        @JvmStatic
        @AfterAll
        fun close() {
            sec.close()
        }
    }
}
