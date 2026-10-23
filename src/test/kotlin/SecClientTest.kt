package com.alexkouzel.sec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SecClientTest {
    @Test
    fun `areas share the client's requests`() {
        val http =
            FixtureHttp(
                "https://www.sec.gov/files/company_tickers_exchange.json" to
                    "files/company_tickers_exchange.json",
            )

        SecClient(http).use { it.companies.all() }

        assertEquals(
            listOf("https://www.sec.gov/files/company_tickers_exchange.json"),
            http.requestedUrls,
        )
    }

    @Test
    fun `rejects a user agent without a contact`() {
        assertFailsWith<IllegalArgumentException> { SecClient("Sample Company") }
    }

    @Test
    fun `close stops further requests`() {
        val client = SecClient("Sample Company admin@example.com")

        client.close()

        assertFailsWith<SecException.Http> { client.companies.all() }
    }
}
