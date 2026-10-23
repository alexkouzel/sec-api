package com.alexkouzel.sec.http

import com.alexkouzel.sec.FixtureHttp
import com.alexkouzel.sec.SecException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpTest {
    private val url = "https://www.sec.gov/files/company_tickers_exchange.json"
    private val http = FixtureHttp(url to "files/company_tickers_exchange.json")

    data class Probe(
        val fields: List<String>,
    )

    @Test
    fun `a parse error names the URL`() {
        val error =
            assertFailsWith<SecException.Parse> {
                http.get(url) { throw SecException.Parse("Unexpected body") }
            }

        assertContains(error.message.orEmpty(), url)
        assertEquals(url, error.url)
    }

    @Test
    fun `unknown fields reach the listener with the URL`() {
        val unknown = mutableListOf<String>()
        http.unknownFields = { url, field -> unknown += "$url $field" }

        http.get(url) { json.map<Probe>(it) }

        assertEquals(listOf("$url HttpTest.Probe.data"), unknown)
    }
}
