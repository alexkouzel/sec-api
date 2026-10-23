package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.FixtureHttp
import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OwnershipTest {
    private val url =
        "https://www.sec.gov/Archives/edgar/data/1000753/000112760223028345/" +
            "0001127602-23-028345.txt"
    private val http = FixtureHttp(url to "ownership/0001127602-23-028345.txt")
    private val ownership = Ownership(http)

    @Test
    fun `get reads the filing the reference points to`() {
        val ref =
            FilingRef("0001127602-23-028345", 1000753, FormType.F4, LocalDate.parse("2023-11-28"))

        val filing = ownership.get(ref)

        assertEquals("INSPERITY, INC.", filing.issuer.name)
        assertEquals(listOf(url), http.requestedUrls)
    }

    @Test
    fun `get reads a filing by CIK and accession number`() {
        val filing = ownership.get(1000753, "0001127602-23-028345")

        assertEquals("0001127602-23-028345", filing.accessionNumber)
        assertEquals(listOf(url), http.requestedUrls)
    }

    @Test
    fun `get rejects a malformed accession number`() {
        assertFailsWith<IllegalArgumentException> { ownership.get(1000753, "000112760223028345") }
    }

    @Test
    fun `get rejects a filing that isn't an ownership form`() {
        val ref =
            FilingRef(
                "0001127602-23-028345",
                1000753,
                FormType.F10_K,
                LocalDate.parse("2023-11-28"),
            )

        assertFailsWith<IllegalArgumentException> { ownership.get(ref) }
    }
}
