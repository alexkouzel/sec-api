package com.alexkouzel.sec.filing

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.SecException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DailyListTest {
    @Test
    fun `parses rows in page order`() {
        val refs = DailyList.parse(Fixtures.bytes("current/current-q1-1.html"))

        assertEquals(
            ref("0000944388-26-000014", 944388, "SCHEDULE 13G", "2026-10-21"),
            refs.first(),
        )
        assertEquals(ref("0002071844-26-001439", 1112996, "40-APP/A", "2026-10-21"), refs.last())
    }

    @Test
    fun `returns each filing once with its first row's CIK`() {
        val refs = DailyList.parse(Fixtures.bytes("current/current-q1-1.html"))

        assertEquals(52, refs.size)
        assertEquals(refs.size, refs.distinctBy { it.accessionNumber }.size)
    }

    @Test
    fun `parses an empty list`() {
        val page = "<pre><strong>Date Filed   Form</strong><hr>\n<hr></pre>"

        assertEquals(emptyList(), DailyList.parse(page.toByteArray()))
    }

    @Test
    fun `rejects an unrecognized row`() {
        val page = "<pre><hr>10-21-2026   something else entirely\n<hr></pre>"

        assertFailsWith<SecException.Parse> { DailyList.parse(page.toByteArray()) }
    }

    @Test
    fun `rejects a page without the list`() {
        val page = "<html><body>No filings</body></html>"

        assertFailsWith<SecException.Parse> { DailyList.parse(page.toByteArray()) }
    }

    @Test
    fun `builds the URL with an encoded form type`() {
        assertEquals(
            "https://www.sec.gov/cgi-bin/current?q1=2&q3=4%2FA",
            DailyList.url(2, FormType.F4_A),
        )
    }

    private fun ref(
        accessionNumber: String,
        cik: Long,
        form: String,
        filedOn: String,
    ) = FilingRef(accessionNumber, cik, FormType(form), LocalDate.parse(filedOn))
}
