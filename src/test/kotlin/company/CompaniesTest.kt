package com.alexkouzel.sec.company

import com.alexkouzel.sec.FixtureHttp
import kotlin.test.Test
import kotlin.test.assertEquals

class CompaniesTest {
    private val url = "https://www.sec.gov/files/company_tickers_exchange.json"
    private val http = FixtureHttp(url to "files/company_tickers_exchange.json")

    @Test
    fun `all reads the company tickers`() {
        val companies = Companies(http).all()

        assertEquals(25, companies.size)
        assertEquals(listOf(url), http.requestedUrls)
    }
}
