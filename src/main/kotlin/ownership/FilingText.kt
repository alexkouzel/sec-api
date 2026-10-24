package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.http.parseDate
import java.time.format.DateTimeFormatter

// The full submission text: an SGML header (<SEC-HEADER>) with values by label, then each
// document with its <FILENAME>; the ownership form's document holds an <XML> block.
internal object FilingText {
    fun url(
        cik: Long,
        accessionNumber: String,
    ): String {
        val folder = accessionNumber.replace("-", "")
        return "https://www.sec.gov/Archives/edgar/data/$cik/$folder/$accessionNumber.txt"
    }

    fun parse(content: ByteArray): OwnershipFiling {
        val text = content.decodeToString()
        val header =
            text.between("<SEC-HEADER>", "</SEC-HEADER>")
                ?: throw SecException.Parse("No <SEC-HEADER>")
        val block = text.between("<XML>", "</XML>") ?: throw SecException.Parse("No <XML> block")

        val accessionNumber = header.value("ACCESSION NUMBER")
        val formType = FormType(header.value("CONFORMED SUBMISSION TYPE"))
        val filedOn = parseDate(header.value("FILED AS OF DATE"), DateTimeFormatter.BASIC_ISO_DATE)
        require(formType in ownershipForms) {
            "Not an ownership filing: $formType ($accessionNumber)"
        }

        return OwnershipXml.parse(
            content = block.trim().toByteArray(),
            accessionNumber = accessionNumber,
            formType = formType,
            filedOn = filedOn,
            document = documentName(text),
        )
    }

    // The <FILENAME> of the document that holds the <XML> block.
    private fun documentName(text: String): String {
        val beforeXml = text.substringBefore("<XML>")
        if ("<FILENAME>" !in beforeXml) {
            throw SecException.Parse("No <FILENAME> for the <XML> block")
        }
        return beforeXml.substringAfterLast("<FILENAME>").substringBefore('\n').trim()
    }

    // Found by label, so added or reordered header lines don't matter.
    private fun String.value(label: String): String {
        val line = Regex("""^${Regex.escape(label)}:[ \t]*(.+?)[ \t]*\r?$""", RegexOption.MULTILINE)
        val match = line.find(this) ?: throw SecException.Parse("No $label in the filing header")
        return match.groupValues[1]
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
