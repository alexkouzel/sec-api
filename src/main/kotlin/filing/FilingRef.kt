package com.alexkouzel.sec.filing

import java.time.LocalDate

/**
 * A filing as a source lists it, without its content.
 *
 * [cik] is one of the CIKs related to the filing; EDGAR serves the filing under each of them.
 * Its role depends on the source: from the latest feed, the Issuer's or Subject's CIK where an
 * entry names one; from [Filings.ofCompany], the requested CIK; otherwise the first CIK the
 * source lists, whose role it doesn't say.
 */
public data class FilingRef(
    val accessionNumber: String,
    val cik: Long,
    val formType: FormType,
    val filedOn: LocalDate,
)
