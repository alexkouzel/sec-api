package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.json
import com.alexkouzel.sec.http.map
import java.time.LocalDate

// A CIK's submissions JSON: `filings.recent` holds the newest filings (at most 1,000 or one
// year), `filings.files` names older files with the same columns.
internal object Submissions {
    private const val CIK_DIGITS = 10

    fun url(cik: Long): String =
        "https://data.sec.gov/submissions/CIK${cik.toString().padStart(CIK_DIGITS, '0')}.json"

    fun olderUrl(file: String): String = "https://data.sec.gov/submissions/$file"

    fun parse(
        content: ByteArray,
        cik: Long,
    ): Page {
        val filings = json.map<Submission>(content).filings
        return Page(filings.recent.refs(cik), filings.files.map { it.name })
    }

    fun parseOlder(
        content: ByteArray,
        cik: Long,
    ): List<FilingRef> = json.map<Columns>(content).refs(cik)

    class Page(
        val refs: List<FilingRef>,
        val olderFiles: List<String>,
    )

    internal data class Submission(
        val filings: SubmissionFilings,
    )

    internal data class SubmissionFilings(
        val recent: Columns,
        val files: List<OlderFile> = emptyList(),
    )

    internal data class OlderFile(
        val name: String,
    )

    internal data class Columns(
        val accessionNumber: List<String>,
        val form: List<String>,
        val filingDate: List<LocalDate>,
    ) {
        fun refs(cik: Long): List<FilingRef> {
            if (form.size != accessionNumber.size || filingDate.size != accessionNumber.size) {
                throw SecException.Parse(
                    "Filing columns differ in length: ${accessionNumber.size} accession numbers, " +
                        "${form.size} forms, ${filingDate.size} filing dates",
                )
            }
            return accessionNumber.indices.map { i ->
                FilingRef(accessionNumber[i], cik, FormType(form[i]), filingDate[i])
            }
        }
    }
}
