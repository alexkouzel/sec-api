package com.alexkouzel.sec.company

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.parseJson
import tools.jackson.databind.JsonNode

// company_tickers_exchange.json: a list of field names, then one array of values per row.
// Columns are found by name, so a reordered or renamed field fails instead of misreading.
internal object Tickers {
    const val URL = "https://www.sec.gov/files/company_tickers_exchange.json"

    private val fields = listOf("cik", "name", "ticker", "exchange")

    fun parse(content: ByteArray): List<Company> {
        val root = parseJson(content)
        val names = root.path("fields").values().map { it.asString() }
        val columns = fields.associateWith { names.indexOf(it) }
        val missing = columns.filterValues { it == -1 }.keys
        if (missing.isNotEmpty()) {
            throw SecException.Parse("Company tickers lack the fields $missing: $names")
        }

        return root.path("data").values().map { company(it, columns) }
    }

    private fun company(
        row: JsonNode,
        columns: Map<String, Int>,
    ): Company {
        fun value(field: String): JsonNode? = row.get(columns.getValue(field))

        fun text(field: String) = value(field)?.takeIf { it.isString }?.stringValue()

        val cik = value("cik")?.takeIf { it.isIntegralNumber }?.longValue()
        val name = text("name")
        val ticker = text("ticker")
        if (cik == null || name == null || ticker == null) {
            throw SecException.Parse("Unrecognized company tickers row: $row")
        }
        return Company(cik, name, ticker, text("exchange"))
    }
}
