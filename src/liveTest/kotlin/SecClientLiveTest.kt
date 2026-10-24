package com.alexkouzel.sec

import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.ownership.FilingText
import com.alexkouzel.sec.ownership.OwnershipFiling
import org.junit.jupiter.api.AfterAll
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Runs every source against live EDGAR, daily in CI (.github/workflows/live.yml). A failure here
// means EDGAR changed or is down; exact values are checked offline against fixtures.
class SecClientLiveTest {
    @Test
    fun `company tickers`() {
        val companies = sec.companies.all()

        assertTrue(companies.size > 5_000, "only ${companies.size} companies")
        assertTrue(companies.any { it.ticker == "AAPL" }, "no AAPL")
    }

    @Test
    fun `filings of a company`() {
        assertValid(sec.filings.company(APPLE))
    }

    @Test
    fun `latest filings`() {
        assertValid(sec.filings.latest())
    }

    @Test
    fun `daily list`() {
        // Empty on weekends and holidays; parsing it is what matters.
        val refs = sec.filings.day(daysAgo = 1)

        refs.forEach(::assertValid)
    }

    @Test
    fun `quarterly index`() {
        assertTrue(previousQuarter.size > 10_000, "only ${previousQuarter.size} filings")
        assertValid(previousQuarter.take(100))
    }

    @Test
    fun `recent ownership filings of every form type`() {
        val problems = mutableListOf<String>()

        for (form in OWNERSHIP_FORMS) {
            val refs = previousQuarter.filter { it.formType == form }.takeLast(SAMPLE_SIZE)
            for (ref in refs) {
                val filing = sec.ownership.get(ref)
                problems +=
                    problems(
                        filing,
                        ref,
                    ).map { "$it in ${FilingText.url(ref.cik, ref.accessionNumber)}" }
            }
        }

        assertEquals(emptyList(), problems)
    }

    // The values users rely on; a renamed element would leave them empty without failing.
    private fun problems(
        filing: OwnershipFiling,
        ref: FilingRef,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (filing.formType !=
            ref.formType
        ) {
            problems += "form type ${filing.formType}, expected ${ref.formType}"
        }
        if (filing.issuer.cik <= 0 || filing.issuer.name == null) problems += "no issuer"
        if (filing.owners.isEmpty()) problems += "no reporting owner"
        for (transaction in filing.transactions) {
            if (transaction.date?.value == null) problems += "a transaction without a date"
            if (transaction.coding?.value == null) problems += "a transaction without a code"
        }
        return problems
    }

    private fun assertValid(refs: List<FilingRef>) {
        assertTrue(refs.isNotEmpty(), "no filings")
        refs.forEach(::assertValid)
    }

    private fun assertValid(ref: FilingRef) {
        assertEquals(20, ref.accessionNumber.length, "accession number ${ref.accessionNumber}")
        assertTrue(ref.cik > 0, "CIK of ${ref.accessionNumber}")
    }

    companion object {
        private const val APPLE = 320193L
        private const val SAMPLE_SIZE = 5
        private val OWNERSHIP_FORMS = with(FormType) { listOf(F3, F3_A, F4, F4_A, F5, F5_A) }

        // EDGAR_USER_AGENT ("Name contact@domain") in CI, a placeholder for local runs.
        private val userAgent =
            System.getenv("EDGAR_USER_AGENT") ?: "TestCompany test.company@gmail.com"
        private val sec = SecClient(userAgent)

        // The last complete quarter has every form type, unlike the current one early on.
        private val previousQuarter: List<FilingRef> by lazy {
            val day = LocalDate.now().minusMonths(3)
            sec.filings.quarter(day.year, (day.monthValue - 1) / 3 + 1)
        }

        @JvmStatic
        @AfterAll
        fun close() {
            sec.close()
        }
    }
}
