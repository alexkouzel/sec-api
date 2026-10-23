package com.alexkouzel.sec.ownership

import java.time.LocalDate

public data class Issuer(
    val cik: Long,
    val name: String?,
    val ticker: String?,
    val foreignTicker: String?,
)

public data class ReportingOwner(
    val cik: Long,
    val name: String?,
    val address: Address?,
    val relationship: Relationship,
)

public data class Address(
    val street1: String?,
    val street2: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val stateDescription: String?,
    val isNonUS: Boolean?,
    val nonUSStateTerritory: String?,
    val country: String?,
)

public data class Relationship(
    val isDirector: Boolean,
    val isOfficer: Boolean,
    val isTenPercentOwner: Boolean,
    val isOther: Boolean,
    val officerTitle: String?,
    val otherText: String?,
)

public data class Signature(
    val name: String?,
    val date: LocalDate?,
)
