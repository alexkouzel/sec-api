package com.alexkouzel.sec.company

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.json
import com.alexkouzel.sec.http.map
import com.alexkouzel.sec.http.unknownField
import tools.jackson.databind.JsonNode

// company_tickers_exchange.json: a list of field names, then one array of values per row.
// Columns are found by name, so a reordered or renamed field fails instead of misreading.
internal object Tickers {
    const val URL = "https://www.sec.gov/files/company_tickers_exchange.json"

    private val fields = listOf("cik", "name", "ticker", "exchange")

    fun parse(content: ByteArray): List<Company> {
        val table = json.map<Table>(content)
        val columns = fields.associateWith { table.fields.indexOf(it) }
        val missing = columns.filterValues { it == -1 }.keys
        if (missing.isNotEmpty()) throw SecException.Parse("Company tickers lack the fields $missing: ${table.fields}")

        (table.fields - fields.toSet()).forEach { unknownField("Tickers.fields.$it") }
        return table.data.map { company(it, columns) }
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

    internal data class Table(
        val fields: List<String>,
        val data: List<JsonNode>,
    )
}
