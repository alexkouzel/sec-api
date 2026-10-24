package com.alexkouzel.sec.filing

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.SecException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubmissionsTest {
    @Test
    fun `parses recent filings`() {
        val refs = Submissions.parse(Fixtures.bytes("submissions/CIK0000946581.json"), 946581).refs

        assertEquals(40, refs.size)
        assertEquals(ref("0000946581-26-000104", 946581, "4", "2026-10-16"), refs.first())
        assertEquals(ref("0001950047-26-006756", 946581, "144", "2026-07-01"), refs.last())
        assertEquals(21, refs.count { it.formType == FormType.F4 })
    }

    @Test
    fun `lists the files of older filings`() {
        val page = Submissions.parse(Fixtures.bytes("submissions/CIK0000946581.json"), 946581)

        assertEquals(listOf("CIK0000946581-submissions-001.json"), page.olderFiles)
    }

    @Test
    fun `parses a file of older filings`() {
        val refs =
            Submissions.parseOlder(
                Fixtures.bytes("submissions/CIK0000946581-submissions-001.json"),
                946581,
            )

        assertEquals(30, refs.size)
        assertEquals(ref("0001104659-15-011425", 946581, "CORRESP", "2015-02-17"), refs.first())
        assertEquals(ref("0001104659-14-078955", 946581, "4", "2014-11-10"), refs.last())
    }

    @Test
    fun `rejects columns of different lengths`() {
        val older =
            """
            {
              "accessionNumber": ["0001104659-15-011425"],
              "form": [],
              "filingDate": ["2015-02-17"]
            }
            """.trimIndent()

        assertFailsWith<SecException.Parse> { Submissions.parseOlder(older.toByteArray(), 946581) }
    }

    @Test
    fun `pads the CIK in the URL`() {
        assertEquals("https://data.sec.gov/submissions/CIK0000946581.json", Submissions.url(946581))
    }
}
