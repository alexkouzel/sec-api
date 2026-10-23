package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class HttpTest {
    @Test
    fun `a parse error names the URL`() {
        val http = Http { "body".toByteArray() }

        val error =
            assertFailsWith<SecException.Parse> {
                http.get("https://www.sec.gov/a") { throw SecException.Parse("Unexpected body") }
            }

        assertContains(error.message.orEmpty(), "https://www.sec.gov/a")
    }
}
