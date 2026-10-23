package com.alexkouzel.sec

/**
 * Told about each field or element in an EDGAR response that this library doesn't know, e.g.
 * `ReportingOwnerAddress.rptOwnerNewThing`: EDGAR added something. Parsing carries on.
 */
public fun interface UnknownFieldListener {
    public fun onUnknownField(
        url: String,
        field: String,
    )
}
