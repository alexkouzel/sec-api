package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.http.Http
import com.alexkouzel.sec.http.get

internal val ownershipForms = with(FormType) { setOf(F3, F3_A, F4, F4_A, F5, F5_A) }

public class Ownership internal constructor(
    private val http: Http,
) {
    /** The Form 3, 4 or 5 filing (or amendment) [ref] points to; one request. */
    public fun get(ref: FilingRef): OwnershipFiling {
        require(ref.formType in ownershipForms) {
            "Not an ownership form: ${ref.formType} (${ref.accessionNumber})"
        }

        return get(ref.cik, ref.accessionNumber)
    }

    /**
     * The Form 3, 4 or 5 filing (or amendment) with this accession number, e.g.
     * `0001127602-23-028345`. [cik] is any CIK related to the filing, such as the issuer's or a
     * reporting owner's; EDGAR serves the filing under each of them. One request.
     */
    public fun get(
        cik: Long,
        accessionNumber: String,
    ): OwnershipFiling {
        require(accessionNumber.matches(ACCESSION_NUMBER)) {
            "Not an accession number (##########-##-######): $accessionNumber"
        }

        return http.get(FilingText.url(cik, accessionNumber), FilingText::parse)
    }

    private companion object {
        val ACCESSION_NUMBER = Regex("""\d{10}-\d{2}-\d{6}""")
    }
}
