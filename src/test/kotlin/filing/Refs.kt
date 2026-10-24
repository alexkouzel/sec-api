package com.alexkouzel.sec.filing

import java.time.LocalDate

// A FilingRef from the values as a fixture shows them.
internal fun ref(
    accessionNumber: String,
    cik: Long,
    form: String,
    filedOn: String,
) = FilingRef(
    accessionNumber = accessionNumber,
    cik = cik,
    formType = FormType(form),
    filedOn = LocalDate.parse(filedOn),
)
