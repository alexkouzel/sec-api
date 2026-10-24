package com.alexkouzel.sec.filing

import com.alexkouzel.sec.FixtureHttp
import com.alexkouzel.sec.SecException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FilingsTest {
    private val latest = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent"
    private val daily = "https://www.sec.gov/cgi-bin/current?q1=1"
    private val company = "https://data.sec.gov/submissions/CIK0000946581.json"
    private val companyOlder = "https://data.sec.gov/submissions/CIK0000946581-submissions-001.json"

    private val http =
        FixtureHttp(
            "$latest&start=0&count=10&output=atom" to "latest/getcurrent-count-10.atom",
            "$latest&start=0&count=40&output=atom&type=4" to
                "latest/getcurrent-count-40-type-4.atom",
            daily to "current/current-q1-1.html",
            "$daily&q3=4" to "current/current-q1-1-q3-4.html",
            "https://www.sec.gov/Archives/edgar/full-index/2026/QTR4/master.idx" to
                "full-index/master-2026-QTR4.idx",
            company to "submissions/CIK0000946581.json",
            companyOlder to "submissions/CIK0000946581-submissions-001.json",
        )
    private val filings = Filings(http)

    @Test
    fun `latest reads the feed`() {
        val refs = filings.latest(count = 10)

        assertEquals(5, refs.size)
        assertEquals(listOf("$latest&start=0&count=10&output=atom"), http.requestedUrls)
    }

    @Test
    fun `latest with a form type keeps exact matches only`() {
        val refs = filings.latest(form = FormType.F4)

        assertEquals(listOf(ref("0001493152-26-048187", 1515139, "4", "2026-10-23")), refs)
    }

    @Test
    fun `latest rejects a count the feed doesn't offer`() {
        assertFailsWith<IllegalArgumentException> { filings.latest(count = 50) }
    }

    @Test
    fun `daily with a form type keeps exact matches only`() {
        val refs = filings.daily(daysAgo = 1, form = FormType.F4)

        assertEquals(33, refs.size)
        assertEquals(ref("0001099910-26-000286", 1553209, "4", "2026-10-21"), refs.first())
        assertTrue(refs.all { it.formType == FormType.F4 })
    }

    @Test
    fun `quarter reads the quarter's index`() {
        val refs = filings.quarter(2026, 4)

        assertEquals(200, refs.size)
    }

    @Test
    fun `quarter rejects a quarter outside 1 to 4`() {
        assertFailsWith<IllegalArgumentException> { filings.quarter(2026, 5) }
    }

    @Test
    fun `company appends older filings`() {
        val refs = filings.company(946581)

        assertEquals(70, refs.size)
        assertEquals(ref("0001104659-15-011425", 946581, "CORRESP", "2015-02-17"), refs[40])
        assertEquals(listOf(company, companyOlder), http.requestedUrls)
    }

    @Test
    fun `a parse error names the URL`() {
        http.route("https://www.sec.gov/cgi-bin/current?q1=2", "full-index/master-2026-QTR4.idx")

        val error = assertFailsWith<SecException.Parse> { filings.daily(daysAgo = 2) }

        assertContains(error.message.orEmpty(), "https://www.sec.gov/cgi-bin/current?q1=2")
    }

    private fun ref(
        accessionNumber: String,
        cik: Long,
        form: String,
        filedOn: String,
    ) = FilingRef(accessionNumber, cik, FormType(form), LocalDate.parse(filedOn))
}
