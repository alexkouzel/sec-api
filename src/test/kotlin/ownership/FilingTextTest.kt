package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FilingTextTest {
    private fun text(accessionNumber: String) =
        Fixtures.bytes("ownership/$accessionNumber.txt").decodeToString()

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "0000921895-26-002730, 1822462, 3,   2026-10-05, X0607, form307422fhtx_10052026.xml",
        "0000929638-26-003820, 2097545, 3,   2026-10-02, X0607, form3.xml",
        "0001000045-22-000004, 1000045, 4,   2022-09-01, X0306, primary_doc.xml",
        "0001062993-22-016446, 1000623, 3,   2022-07-18, X0206, form3.xml",
        "0001062993-26-005298, 1099369, 5,   2026-10-13, X0609, form5.xml",
        "0001127602-23-028345, 1000753, 4,   2023-11-28, X0508, form4.xml",
        "0001193125-26-427843, 2111860, 3/A, 2026-10-21, X0607, ownership.xml",
        "0001225208-26-008370, 1300391, 5,   2026-10-13, X0609, doc5.xml",
        "0001315098-26-000186, 1315098, 4,   2026-10-02, X0609, wk-form4_1790978110.xml",
        "0001437749-22-017271,   69891, 5/A, 2022-07-18, X0306, rdgdoc.xml",
        "0001437749-22-020350,  100378, 5,   2022-08-12, X0306, rdgdoc.xml",
        "0001493152-26-046274, 1729173, 4,   2026-10-08, X0609, ownership.xml",
        "0001701247-26-000014, 1672619, 4,   2026-10-21, X0609, form4-10212026_091032.xml",
        "0001887616-26-000024, 1819989, 4/A, 2026-10-08, X0609, wk-form4a_1791500258.xml",
    )
    fun `parses the header and the form`(
        accessionNumber: String,
        issuerCik: Long,
        form: String,
        filedOn: String,
        schemaVersion: String,
        document: String,
    ) {
        val filing = FilingText.parse(Fixtures.bytes("ownership/$accessionNumber.txt"))

        assertEquals(accessionNumber, filing.accessionNumber)
        assertEquals(FormType(form), filing.formType)
        assertEquals(LocalDate.parse(filedOn), filing.filedOn)
        assertEquals(document, filing.document)
        assertEquals(schemaVersion, filing.schemaVersion)
        assertEquals(issuerCik, filing.issuer.cik)
        assertTrue(filing.owners.isNotEmpty())
    }

    @Test
    fun `finds header values by label`() {
        val original = text("0001127602-23-028345")
        val text =
            original.replace(
                "ACCESSION NUMBER:",
                "SOME NEW FIELD:\t\tvalue\nACCESSION NUMBER:",
            )

        val filing = FilingText.parse(text.toByteArray())

        assertEquals("0001127602-23-028345", filing.accessionNumber)
        assertEquals(FormType.F4, filing.formType)
        assertEquals(LocalDate.parse("2023-11-28"), filing.filedOn)
    }

    @Test
    fun `rejects a header without a value`() {
        val text = text("0001127602-23-028345").replace("FILED AS OF DATE:", "FILED ON:")

        assertFailsWith<SecException.Parse> { FilingText.parse(text.toByteArray()) }
    }

    @Test
    fun `rejects a filing without an XML block`() {
        val text = text("0001127602-23-028345").replace("<XML>", "<TEXT>")

        assertFailsWith<SecException.Parse> { FilingText.parse(text.toByteArray()) }
    }

    @Test
    fun `rejects a filing that isn't an ownership form`() {
        val original = text("0001127602-23-028345")
        val text =
            original.replace(
                "CONFORMED SUBMISSION TYPE:\t4",
                "CONFORMED SUBMISSION TYPE:\t10-K",
            )

        assertFailsWith<IllegalArgumentException> { FilingText.parse(text.toByteArray()) }
    }

    @Test
    fun `builds the URL under the CIK and the accession number`() {
        val url = FilingText.url(1000753, "0001127602-23-028345")

        assertEquals(
            "https://www.sec.gov/Archives/edgar/data/1000753/000112760223028345/" +
                "0001127602-23-028345.txt",
            url,
        )
    }
}
