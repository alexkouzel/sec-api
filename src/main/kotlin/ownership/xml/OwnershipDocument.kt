package com.alexkouzel.sec.ownership.xml

import com.fasterxml.jackson.annotation.JsonMerge
import tools.jackson.dataformat.xml.annotation.JacksonXmlText

// Mirrors the ownership XSD. Every leaf is kept as text and converted in Conversion.kt.
// Repeated elements may be interleaved with others (valid per XSD); @JsonMerge keeps every
// group instead of letting a later one replace the earlier ones.

internal class OwnershipDocument {
    var schemaVersion: String? = null
    var documentType: String? = null
    var periodOfReport: String? = null
    var dateOfOriginalSubmission: String? = null
    var noSecuritiesOwned: String? = null
    var notSubjectToSection16: String? = null
    var form3HoldingsReported: String? = null
    var form4TransactionsReported: String? = null
    var aff10b5One: String? = null
    var issuer: Issuer? = null

    @field:JsonMerge
    var reportingOwner: MutableList<ReportingOwner> = mutableListOf()
    var nonDerivativeTable: NonDerivativeTable? = null
    var derivativeTable: DerivativeTable? = null
    var footnotes: Footnotes? = null
    var remarks: String? = null

    @field:JsonMerge
    var ownerSignature: MutableList<OwnerSignature> = mutableListOf()
}

internal class Issuer {
    var issuerCik: String? = null
    var issuerName: String? = null
    var issuerTradingSymbol: String? = null
    var issuerForeignTradingSymbol: String? = null
}

internal class ReportingOwner {
    var reportingOwnerId: ReportingOwnerId? = null
    var reportingOwnerAddress: ReportingOwnerAddress? = null
    var reportingOwnerRelationship: ReportingOwnerRelationship? = null
}

internal class ReportingOwnerId {
    var rptOwnerCik: String? = null
    var rptOwnerCcc: String? = null
    var rptOwnerName: String? = null
}

internal class ReportingOwnerAddress {
    var rptOwnerStreet1: String? = null
    var rptOwnerStreet2: String? = null
    var rptOwnerCity: String? = null
    var rptOwnerState: String? = null
    var rptOwnerZipCode: String? = null
    var rptOwnerStateDescription: String? = null
    var rptOwnerNonUSAddressFlag: String? = null
    var rptOwnerNonUSStateTerritory: String? = null
    var rptOwnerCountry: String? = null
    var rptOwnerGoodAddress: String? = null
}

// Constructor properties keep the XSD's "is" names; Kotlin would turn a var isDirector into
// the Java property "director".
internal data class ReportingOwnerRelationship(
    val isDirector: String? = null,
    val isOfficer: String? = null,
    val isTenPercentOwner: String? = null,
    val isOther: String? = null,
    val officerTitle: String? = null,
    val otherText: String? = null,
)

internal class OwnerSignature {
    var signatureName: String? = null
    var signatureDate: String? = null
}

internal class Footnotes {
    @field:JsonMerge
    var footnote: MutableList<Footnote> = mutableListOf()
}

internal class Footnote {
    var id: String? = null

    @field:JacksonXmlText
    var text: String? = null
}

internal data class FootnoteId(
    val id: String? = null,
)

// A value element: <value> and <footnoteId>s, either of them optional.
internal class Value {
    var value: String? = null

    @field:JsonMerge
    var footnoteId: MutableList<FootnoteId> = mutableListOf()

    companion object {
        // Some filings put the bare value, or nothing, in place of <value> and <footnoteId>.
        @JvmStatic
        fun valueOf(text: String): Value = Value().apply { value = text }
    }
}
