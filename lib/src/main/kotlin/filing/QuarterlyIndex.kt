package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import java.time.LocalDate

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
        if (separator == -1) throw SecException.Parse("No line of dashes before the rows of the quarterly index")

        return lines
            .drop(separator + 1)
            .filter { it.isNotBlank() }
            .map(::parseRow)
            .distinctBy { it.accessionNumber }
    }

    private fun parseRow(text: String): FilingRef {
        val columns = text.split('|').map { it.trim() }
        if (columns.size != COLUMNS) throw SecException.Parse("Expected $COLUMNS columns in quarterly index row: $text")

        val (cik, _, form, filedOn, path) = columns
        return FilingRef(
            accessionNumber = path.substringAfterLast('/').removeSuffix(".txt"),
            cik = cik.toLongOrNull() ?: throw SecException.Parse("Invalid CIK in quarterly index row: $text"),
            formType = FormType(form),
            filedOn = LocalDate.parse(filedOn),
        )
    }
}
