package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.json
import com.alexkouzel.sec.http.map
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

// A CIK's submissions JSON: `filings.recent` holds the newest filings (at most 1,000 or one
// year), `filings.files` names older files with the same columns.
internal object Submissions {
    private const val CIK_DIGITS = 10

    fun url(cik: Long): String = "https://data.sec.gov/submissions/CIK${cik.toString().padStart(CIK_DIGITS, '0')}.json"

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

    // Every field EDGAR sends, so a new one is reported as unknown; only some are read.
    internal data class Submission(
        val cik: Any? = null,
        val entityType: Any? = null,
        val sic: Any? = null,
        val sicDescription: Any? = null,
        val ownerOrg: Any? = null,
        val insiderTransactionForOwnerExists: Any? = null,
        val insiderTransactionForIssuerExists: Any? = null,
        val name: Any? = null,
        val tickers: Any? = null,
        val exchanges: Any? = null,
        val ein: Any? = null,
        val lei: Any? = null,
        val description: Any? = null,
        val website: Any? = null,
        val investorWebsite: Any? = null,
        val category: Any? = null,
        val fiscalYearEnd: Any? = null,
        val stateOfIncorporation: Any? = null,
        val stateOfIncorporationDescription: Any? = null,
        val addresses: Map<String, Address> = emptyMap(),
        val phone: Any? = null,
        val flags: Any? = null,
        val formerNames: List<FormerName> = emptyList(),
        val filings: SubmissionFilings,
    )

    internal data class Address(
        val street1: Any? = null,
        val street2: Any? = null,
        val city: Any? = null,
        val stateOrCountry: Any? = null,
        val zipCode: Any? = null,
        val stateOrCountryDescription: Any? = null,
        val isForeignLocation: Any? = null,
        val foreignStateTerritory: Any? = null,
        val country: Any? = null,
        val countryCode: Any? = null,
    )

    internal data class FormerName(
        val name: Any? = null,
        val from: Any? = null,
        val to: Any? = null,
    )

    internal data class SubmissionFilings(
        val recent: Columns,
        val files: List<OlderFile> = emptyList(),
    )

    internal data class OlderFile(
        val name: String,
        val filingCount: Any? = null,
        val filingFrom: Any? = null,
        val filingTo: Any? = null,
    )

    internal data class Columns(
        val accessionNumber: List<String>,
        val form: List<String>,
        val filingDate: List<LocalDate>,
        val reportDate: Any? = null,
        val acceptanceDateTime: Any? = null,
        val act: Any? = null,
        val fileNumber: Any? = null,
        val filmNumber: Any? = null,
        val items: Any? = null,
        @param:JsonProperty("core_type") val coreType: Any? = null,
        val size: Any? = null,
        val isXBRL: Any? = null,
        val isInlineXBRL: Any? = null,
        val isXBRLNumeric: Any? = null,
        val primaryDocument: Any? = null,
        val primaryDocDescription: Any? = null,
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
