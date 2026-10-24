package com.alexkouzel.sec.filing

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.SecException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuarterlyIndexTest {
    @Test
    fun `parses the current index`() {
        val refs = QuarterlyIndex.parse(Fixtures.bytes("full-index/master-2026-QTR4.idx"))

        assertEquals(200, refs.size)
        assertEquals(ref("0000919574-26-006682", 1000177, "4", "2026-10-06"), refs.first())
        assertEquals(ref("0000950103-26-015862", 1000275, "424B2", "2026-10-20"), refs.last())
    }

    @Test
    fun `parses an older index`() {
        val refs = QuarterlyIndex.parse(Fixtures.bytes("full-index/master-2022-QTR3.idx"))

        assertEquals(200, refs.size)
        assertEquals(ref("0000950170-22-016293", 1000045, "10-Q", "2022-08-10"), refs.first())
        assertEquals(14, refs.count { it.formType == FormType.F4 })
    }

    @Test
    fun `finds the rows after any header`() {
        val index =
            """
            Description: Master Index
            An extra header line

            CIK|Company Name|Form Type|Date Filed|Filename
            --------------------------------------------------------------------------------
            1000177|NORDIC AMERICAN TANKERS Ltd|4|2026-10-06|edgar/data/1000177/0000919574-26-006682.txt
            """.trimIndent()

        assertEquals(
            listOf(ref("0000919574-26-006682", 1000177, "4", "2026-10-06")),
            QuarterlyIndex.parse(index.toByteArray()),
        )
    }

    @Test
    fun `returns each filing once with its first row's CIK`() {
        val index =
            """
            -----
            1000177|NORDIC AMERICAN TANKERS Ltd|4|2026-10-06|edgar/data/1000177/0000919574-26-006682.txt
            1234567|SMITH JOHN|4|2026-10-06|edgar/data/1234567/0000919574-26-006682.txt
            """.trimIndent()

        assertEquals(
            listOf(ref("0000919574-26-006682", 1000177, "4", "2026-10-06")),
            QuarterlyIndex.parse(index.toByteArray()),
        )
    }

    @Test
    fun `rejects an index without the line of dashes`() {
        val index = "CIK|Company Name|Form Type|Date Filed|Filename\n"

        assertFailsWith<SecException.Parse> { QuarterlyIndex.parse(index.toByteArray()) }
    }

    @Test
    fun `rejects a row with other columns`() {
        val index = "-----\n1000177|NORDIC AMERICAN TANKERS Ltd|4|2026-10-06\n"

        assertFailsWith<SecException.Parse> { QuarterlyIndex.parse(index.toByteArray()) }
    }
}
