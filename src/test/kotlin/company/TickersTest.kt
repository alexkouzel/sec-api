package com.alexkouzel.sec.company

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.SecException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TickersTest {
    @Test
    fun `parses every row in order`() {
        val companies = Tickers.parse(Fixtures.bytes("files/company_tickers_exchange.json"))

        assertEquals(25, companies.size)
        assertEquals(Company(1045810, "NVIDIA CORP", "NVDA", "Nasdaq"), companies[0])
        assertEquals(Company(320193, "Apple Inc.", "AAPL", "Nasdaq"), companies[1])
        assertEquals(Company(858877, "CISCO SYSTEMS, INC.", "CSCO", "Nasdaq"), companies.last())
    }

    @Test
    fun `finds columns by field name`() {
        val tickers =
            """{"fields": ["ticker", "exchange", "name", "cik"], "data": [["AAPL", "Nasdaq", "Apple Inc.", 320193]]}"""

        assertEquals(
            listOf(Company(320193, "Apple Inc.", "AAPL", "Nasdaq")),
            Tickers.parse(tickers.toByteArray()),
        )
    }

    @Test
    fun `reads a missing exchange as null`() {
        val tickers =
            """{"fields": ["cik", "name", "ticker", "exchange"], "data": [[1234567, "Some Fund", "SFX", null]]}"""

        assertEquals(
            listOf(Company(1234567, "Some Fund", "SFX", null)),
            Tickers.parse(tickers.toByteArray()),
        )
    }

    @Test
    fun `rejects tickers without a known field`() {
        val tickers = """{"fields": ["cik", "title", "ticker", "exchange"], "data": []}"""

        assertFailsWith<SecException.Parse> { Tickers.parse(tickers.toByteArray()) }
    }

    @Test
    fun `rejects a row whose CIK isn't a number`() {
        val tickers =
            """{"fields": ["cik", "name", "ticker", "exchange"], "data": [["0000320193", "Apple", "AAPL", "Nasdaq"]]}"""

        assertFailsWith<SecException.Parse> { Tickers.parse(tickers.toByteArray()) }
    }
}
