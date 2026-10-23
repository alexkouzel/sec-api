package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.http.Http
import com.alexkouzel.sec.http.get

public class Ownership internal constructor(
    private val http: Http,
) {
    /** The Form 3, 4 or 5 filing (or amendment) [ref] points to; one request. */
    public fun get(ref: FilingRef): OwnershipFiling {
        require(ref.formType in forms) { "Not an ownership form: ${ref.formType} (${ref.accessionNumber})" }

        return http.get(FilingText.url(ref), FilingText::parse)
    }

    private companion object {
        val forms = with(FormType) { setOf(F3, F3_A, F4, F4_A, F5, F5_A) }
    }
}
