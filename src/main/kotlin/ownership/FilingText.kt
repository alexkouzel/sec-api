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
import java.time.format.DateTimeParseException

// The full submission text: an SGML header (<SEC-HEADER>) with values by label, then each
// document with its <FILENAME>; the ownership form's document holds an <XML> block.
internal object FilingText {
    fun url(ref: FilingRef): String {
        val folder = ref.accessionNumber.replace("-", "")
        val file = "${ref.accessionNumber}.txt"
        return "https://www.sec.gov/Archives/edgar/data/${ref.cik}/$folder/$file"
    }

    fun parse(content: ByteArray): OwnershipFiling {
        val text = content.decodeToString()
        val header =
            text.between("<SEC-HEADER>", "</SEC-HEADER>")
                ?: throw SecException.Parse("No <SEC-HEADER>")
        val block = text.between("<XML>", "</XML>") ?: throw SecException.Parse("No <XML> block")

        val ownershipDocument = xml.map<OwnershipDocument>(block.trim().toByteArray())
        return ownershipDocument.toFiling(
            accessionNumber = header.value("ACCESSION NUMBER"),
            formType = FormType(header.value("CONFORMED SUBMISSION TYPE")),
            filedOn = filedOn(header.value("FILED AS OF DATE")),
            document = documentName(text),
        )
    }

    // The <FILENAME> of the document that holds the <XML> block.
    private fun documentName(text: String): String {
        val beforeXml = text.substringBefore("<XML>")
        return beforeXml.substringAfterLast("<FILENAME>", "").substringBefore('\n').trim()
    }

    // Found by label, so added or reordered header lines don't matter.
    private fun String.value(label: String): String {
        val line = Regex("""^${Regex.escape(label)}:[ \t]*(.+?)[ \t]*\r?$""", RegexOption.MULTILINE)
        val match = line.find(this) ?: throw SecException.Parse("No $label in the filing header")
        return match.groupValues[1]
    }

    private fun filedOn(value: String): LocalDate {
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (e: DateTimeParseException) {
            throw SecException.Parse("Invalid FILED AS OF DATE: $value", e)
        }
    }

    private fun String.between(
        open: String,
        close: String,
    ): String? {
        val start = indexOf(open)
        val end = if (start == -1) -1 else indexOf(close, start + open.length)
        if (end == -1) return null
        return substring(start + open.length, end)
    }
}
