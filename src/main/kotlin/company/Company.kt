package com.alexkouzel.sec.company

/** A company with a ticker; [exchange] is null where EDGAR lists none. */
public data class Company(
    val cik: Long,
    val name: String,
    val ticker: String,
    val exchange: String?,
)
