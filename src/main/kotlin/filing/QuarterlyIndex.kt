package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.parseDate

// master.idx: a free-form header, a line of dashes, then rows of
// CIK|Company Name|Form Type|Date Filed|Filename, one per filing and related CIK.
internal object QuarterlyIndex {
    private const val COLUMNS = 5

    fun url(
        year: Int,
        quarter: Int,
    ): String = "https://www.sec.gov/Archives/edgar/full-index/$year/QTR$quarter/master.idx"

    fun parse(content: ByteArray): List<FilingRef> {
        val lines = content.decodeToString().lines()
        val separator = lines.indexOfFirst { it.startsWith("---") }
        if (separator == -1) {
            throw SecException.Parse("No line of dashes before the rows of the quarterly index")
        }

        val rows = lines.drop(separator + 1).filter { it.isNotBlank() }
        return rows.map(::ref).distinctBy { it.accessionNumber }
    }

    private fun ref(line: String): FilingRef {
        val columns = line.split('|').map { it.trim() }
        if (columns.size != COLUMNS) {
            throw SecException.Parse("Expected $COLUMNS columns in quarterly index row: $line")
        }

        val (cik, _, form, filedOn, path) = columns
        val accessionNumber = path.substringAfterLast('/').removeSuffix(".txt")
        val cikValue =
            cik.toLongOrNull()
                ?: throw SecException.Parse("Invalid CIK in quarterly index row: $line")
        return FilingRef(
            accessionNumber = accessionNumber,
            cik = cikValue,
            formType = FormType(form),
            filedOn = parseDate(filedOn),
        )
    }
}
