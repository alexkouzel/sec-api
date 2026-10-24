package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.isoDate
import com.alexkouzel.sec.http.parseJson
import tools.jackson.databind.JsonNode

// A CIK's submissions JSON: `filings.recent` holds the newest filings (at most 1,000 or one
// year) as parallel columns, `filings.files` names older files with the same columns.
internal object Submissions {
    private const val CIK_DIGITS = 10

    fun url(cik: Long): String {
        val paddedCik = cik.toString().padStart(CIK_DIGITS, '0')
        return "https://data.sec.gov/submissions/CIK$paddedCik.json"
    }

    fun olderUrl(file: String): String = "https://data.sec.gov/submissions/$file"

    fun parse(
        content: ByteArray,
        cik: Long,
    ): Page {
        val filings = parseJson(content).path("filings")
        val refs = refs(filings.path("recent"), cik)
        val olderFiles = filings.path("files").values().map { it.path("name").asString() }
        return Page(refs, olderFiles)
    }

    fun parseOlder(
        content: ByteArray,
        cik: Long,
    ): List<FilingRef> = refs(parseJson(content), cik)

    private fun refs(
        columns: JsonNode,
        cik: Long,
    ): List<FilingRef> {
        val accessionNumbers = column(columns, "accessionNumber")
        val forms = column(columns, "form")
        val filingDates = column(columns, "filingDate")
        if (forms.size != accessionNumbers.size || filingDates.size != accessionNumbers.size) {
            throw SecException.Parse(
                "Filing columns differ in length: ${accessionNumbers.size} accession numbers, " +
                    "${forms.size} forms, ${filingDates.size} filing dates",
            )
        }
        return accessionNumbers.indices.map { i ->
            FilingRef(accessionNumbers[i], cik, FormType(forms[i]), isoDate(filingDates[i]))
        }
    }

    private fun column(
        columns: JsonNode,
        name: String,
    ): List<String> {
        val column = columns.path(name)
        if (!column.isArray) {
            throw SecException.Parse("No $name column in the submissions")
        }
        return column.values().map { it.asString() }
    }

    class Page(
        val refs: List<FilingRef>,
        val olderFiles: List<String>,
    )
}
