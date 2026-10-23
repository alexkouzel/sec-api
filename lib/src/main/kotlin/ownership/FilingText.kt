package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.http.map
import com.alexkouzel.sec.http.xml
import com.alexkouzel.sec.ownership.xml.OwnershipDocument
import com.alexkouzel.sec.ownership.xml.toFiling
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The full submission text: an SGML header (<SEC-HEADER>) with values by label, then each
// document with its <FILENAME>; the ownership form's document holds an <XML> block.
internal object FilingText {
    private val basicDate = DateTimeFormatter.BASIC_ISO_DATE

    fun url(ref: FilingRef): String {
        val folder = ref.accessionNumber.replace("-", "")
        return "https://www.sec.gov/Archives/edgar/data/${ref.cik}/$folder/${ref.accessionNumber}.txt"
    }

    fun parse(content: ByteArray): OwnershipFiling {
        val text = content.decodeToString()
        val header = text.between("<SEC-HEADER>", "</SEC-HEADER>") ?: throw SecException.Parse("No <SEC-HEADER>")
        val xmlStart = text.indexOf("<XML>")
        val block = text.between("<XML>", "</XML>") ?: throw SecException.Parse("No <XML> block")
        val document =
            text
                .substring(0, xmlStart)
                .substringAfterLast("<FILENAME>", "")
                .substringBefore('\n')
                .trim()

        return xml.map<OwnershipDocument>(block.trim().toByteArray()).toFiling(
            accessionNumber = header.value("ACCESSION NUMBER"),
            formType = FormType(header.value("CONFORMED SUBMISSION TYPE")),
            filedOn = filedOn(header.value("FILED AS OF DATE")),
            document = document,
        )
    }

    // Found by label, so added or reordered header lines don't matter.
    private fun String.value(label: String): String =
        Regex("""^${Regex.escape(label)}:[ \t]*(.+?)[ \t]*\r?$""", RegexOption.MULTILINE)
            .find(this)
            ?.groupValues
            ?.get(1)
            ?: throw SecException.Parse("No $label in the filing header")

    private fun filedOn(value: String): LocalDate =
        runCatching { LocalDate.parse(value, basicDate) }
            .getOrElse { throw SecException.Parse("Invalid FILED AS OF DATE: $value", cause = it) }

    private fun String.between(
        open: String,
        close: String,
    ): String? {
        val start = indexOf(open)
        val end = if (start == -1) -1 else indexOf(close, start + open.length)
        return if (end == -1) null else substring(start + open.length, end)
    }
}
