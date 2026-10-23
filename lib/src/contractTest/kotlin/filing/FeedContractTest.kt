package com.alexkouzel.sec.filing

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.company.Tickers
import com.alexkouzel.sec.http.collectingUnknownFields
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals

// The JSON and Atom fixtures have only fields the wire classes know, so the library's
// unknown-field listener stays quiet until EDGAR adds one.
class FeedContractTest {
    private fun unknownFields(parse: () -> Unit) =
        mutableListOf<String>().also { collectingUnknownFields(it::add, parse) }

    @Test
    fun `submissions have only known fields`() {
        val unknown = unknownFields { Submissions.parse(Fixtures.bytes("submissions/CIK0000946581.json"), 946581) }

        assertEquals(emptyList(), unknown)
    }

    @Test
    fun `older submissions have only known fields`() {
        val unknown =
            unknownFields {
                Submissions.parseOlder(
                    Fixtures.bytes("submissions/CIK0000946581-submissions-001.json"),
                    946581,
                )
            }

        assertEquals(emptyList(), unknown)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "getcurrent-count-10.atom",
            "getcurrent-count-40-type-4.atom",
            "getcurrent-count-20-type-SCHEDULE13.atom",
        ],
    )
    fun `the latest feed has only known elements`(path: String) {
        val unknown = unknownFields { LatestFeed.parse(Fixtures.bytes("latest/$path")) }

        assertEquals(emptyList(), unknown)
    }

    @Test
    fun `company tickers have only known fields`() {
        val unknown = unknownFields { Tickers.parse(Fixtures.bytes("files/company_tickers_exchange.json")) }

        assertEquals(emptyList(), unknown)
    }

    @Test
    fun `a new field is reported by name`() {
        val json =
            Fixtures
                .bytes("submissions/CIK0000946581.json")
                .decodeToString()
                .replace("\"accessionNumber\":", "\"newColumn\":[1],\"accessionNumber\":")
                .replaceFirst("\"cik\":", "\"newTopLevel\":{\"a\":1},\"cik\":")

        val unknown = unknownFields { Submissions.parse(json.toByteArray(), 946581) }

        assertEquals(setOf("Submissions.Submission.newTopLevel", "Submissions.Columns.newColumn"), unknown.toSet())
    }
}
