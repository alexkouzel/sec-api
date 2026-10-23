package com.alexkouzel.sec.filing

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.SecException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LatestFeedTest {
    private fun parse(path: String) = LatestFeed.parse(Fixtures.bytes("latest/$path"))

    @Test
    fun `parses entries in feed order`() {
        val refs = parse("getcurrent-count-10.atom")

        assertEquals(ref("0001213900-26-111931", 2028201, "15-12G", "2026-10-23"), refs.first())
        assertEquals(ref("0001193125-26-429780", 2086532, "40-17G", "2026-10-23"), refs.last())
    }

    @Test
    fun `returns each filing once`() {
        val refs = parse("getcurrent-count-10.atom")

        assertEquals(5, refs.size)
        assertEquals(refs.size, refs.distinctBy { it.accessionNumber }.size)
    }

    @Test
    fun `keeps the issuer's CIK over the reporting owner's`() {
        val refs = parse("getcurrent-count-40-type-4.atom")

        assertEquals(
            listOf(ref("0001493152-26-048187", 1515139, "4", "2026-10-23")),
            refs.filter { it.accessionNumber == "0001493152-26-048187" },
        )
    }

    @Test
    fun `keeps the subject company's CIK in either entry order`() {
        val refs = parse("getcurrent-count-20-type-SCHEDULE13.atom")

        assertEquals(10, refs.size)
        assertEquals(
            ref("0000038777-26-000271", 1762562, "SCHEDULE 13D/A", "2026-10-23"),
            refs.first(),
        )
        assertContains(refs, ref("0001493152-26-048185", 1986694, "SCHEDULE 13D", "2026-10-23"))
    }

    @Test
    fun `parses an empty feed`() {
        val feed =
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Latest Filings</title>
            </feed>
            """.trimIndent()

        assertEquals(emptyList(), LatestFeed.parse(feed.toByteArray()))
    }

    @Test
    fun `rejects an entry without an accession number`() {
        val feed =
            """
            <feed xmlns="http://www.w3.org/2005/Atom"><entry>
            <title>4 - WASTE ENERGY CORP. (0001515139) (Issuer)</title>
            <link rel="alternate" type="text/html" href="https://www.sec.gov/Archives/edgar/data/1515139/x-index.htm"/>
            <summary type="html"> &lt;b&gt;Filed:&lt;/b&gt; 2026-10-23</summary>
            <category scheme="https://www.sec.gov/" label="form type" term="4"/>
            <id>urn:tag:sec.gov,2008:something-else</id>
            </entry></feed>
            """.trimIndent()

        assertFailsWith<SecException.Parse> { LatestFeed.parse(feed.toByteArray()) }
    }

    @Test
    fun `builds the URL with an encoded form type`() {
        assertEquals(
            "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent" +
                "&start=0&count=40&output=atom&type=SCHEDULE+13D%2FA",
            LatestFeed.url(0, 40, FormType("SCHEDULE 13D/A")),
        )
    }

    private fun ref(
        accessionNumber: String,
        cik: Long,
        form: String,
        filedOn: String,
    ) = FilingRef(accessionNumber, cik, FormType(form), LocalDate.parse(filedOn))
}
