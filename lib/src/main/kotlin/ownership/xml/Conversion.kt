package com.alexkouzel.sec.ownership.xml

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.ownership.Address
import com.alexkouzel.sec.ownership.Coding
import com.alexkouzel.sec.ownership.Derivative
import com.alexkouzel.sec.ownership.Footnoted
import com.alexkouzel.sec.ownership.Holding
import com.alexkouzel.sec.ownership.OwnershipFiling
import com.alexkouzel.sec.ownership.Relationship
import com.alexkouzel.sec.ownership.Signature
import com.alexkouzel.sec.ownership.Transaction
import java.time.LocalDate
import com.alexkouzel.sec.ownership.Footnote as FootnoteModel
import com.alexkouzel.sec.ownership.Issuer as IssuerModel
import com.alexkouzel.sec.ownership.ReportingOwner as ReportingOwnerModel

internal fun OwnershipDocument.toFiling(
    accessionNumber: String,
    formType: FormType,
    filedOn: LocalDate,
    document: String,
): OwnershipFiling =
    OwnershipFiling(
        accessionNumber = accessionNumber,
        formType = formType,
        filedOn = filedOn,
        document = document,
        schemaVersion = schemaVersion.text(),
        periodOfReport = periodOfReport.date(),
        dateOfOriginalSubmission = dateOfOriginalSubmission.date(),
        issuer = issuer?.toModel() ?: throw SecException.Parse("No <issuer> in the ownership document"),
        owners = reportingOwner.map { it.toModel() },
        transactions =
            nonDerivativeTable?.nonDerivativeTransaction.orEmpty().map { it.toModel(derivative = null) } +
                derivativeTable?.derivativeTransaction.orEmpty().map { it.toModel(it.derivative()) },
        holdings =
            nonDerivativeTable?.nonDerivativeHolding.orEmpty().map { it.toModel(derivative = null) } +
                derivativeTable?.derivativeHolding.orEmpty().map { it.toModel(it.derivative()) },
        footnotes = footnotes?.footnote.orEmpty().map { it.toModel() },
        signatures = ownerSignature.map { Signature(it.signatureName.text(), it.signatureDate.date()) },
        remarks = remarks.text(),
        ownsNoSecurities = noSecuritiesOwned.flag() ?: false,
        isNotSubjectToSection16 = notSubjectToSection16.flag() ?: false,
        hasForm3HoldingsReported = form3HoldingsReported.flag(),
        hasForm4TransactionsReported = form4TransactionsReported.flag(),
        isRule10b5One = aff10b5One.flag(),
    )

private fun Issuer.toModel() =
    IssuerModel(
        cik = issuerCik.cik("issuerCik"),
        name = issuerName.text(),
        ticker = issuerTradingSymbol.text(),
        foreignTicker = issuerForeignTradingSymbol.text(),
    )

private fun ReportingOwner.toModel(): ReportingOwnerModel {
    val relationship = reportingOwnerRelationship ?: ReportingOwnerRelationship()
    return ReportingOwnerModel(
        cik = reportingOwnerId?.rptOwnerCik.cik("rptOwnerCik"),
        name = reportingOwnerId?.rptOwnerName.text(),
        address = reportingOwnerAddress?.toModel(),
        relationship =
            Relationship(
                isDirector = relationship.isDirector.flag() ?: false,
                isOfficer = relationship.isOfficer.flag() ?: false,
                isTenPercentOwner = relationship.isTenPercentOwner.flag() ?: false,
                isOther = relationship.isOther.flag() ?: false,
                officerTitle = relationship.officerTitle.text(),
                otherText = relationship.otherText.text(),
            ),
    )
}

private fun ReportingOwnerAddress.toModel() =
    Address(
        street1 = rptOwnerStreet1.text(),
        street2 = rptOwnerStreet2.text(),
        city = rptOwnerCity.text(),
        state = rptOwnerState.text(),
        zipCode = rptOwnerZipCode.text(),
        stateDescription = rptOwnerStateDescription.text(),
        isNonUS = rptOwnerNonUSAddressFlag.flag(),
        nonUSStateTerritory = rptOwnerNonUSStateTerritory.text(),
        country = rptOwnerCountry.text(),
    )

private fun Footnote.toModel() =
    FootnoteModel(
        id = id.text() ?: throw SecException.Parse("A footnote has no id"),
        text = text.text().orEmpty(),
    )

private fun NonDerivativeTransaction.toModel(derivative: Derivative?) =
    Transaction(
        securityTitle = securityTitle.footnoted { it },
        date = transactionDate.footnoted(::parseDate),
        deemedExecutionDate = deemedExecutionDate.footnoted(::parseDate),
        coding = transactionCoding?.toModel(),
        timeliness = transactionTimeliness.footnoted { it },
        shares = transactionAmounts?.transactionShares.footnoted(::number),
        pricePerShare = transactionAmounts?.transactionPricePerShare.footnoted(::number),
        totalValue = transactionAmounts?.transactionTotalValue.footnoted(::number),
        acquiredOrDisposed = transactionAmounts?.transactionAcquiredDisposedCode.footnoted { it },
        sharesOwnedAfter = postTransactionAmounts?.sharesOwnedFollowingTransaction.footnoted(::number),
        valueOwnedAfter = postTransactionAmounts?.valueOwnedFollowingTransaction.footnoted(::number),
        directOrIndirect = ownershipNature?.directOrIndirectOwnership.footnoted { it },
        natureOfOwnership = ownershipNature?.natureOfOwnership.footnoted { it },
        derivative = derivative,
    )

private fun NonDerivativeHolding.toModel(derivative: Derivative?) =
    Holding(
        securityTitle = securityTitle.footnoted { it },
        formType =
            transactionCoding?.let { coding ->
                footnoted(coding.transactionFormType.text()?.let(::FormType), coding.footnoteId)
            },
        sharesOwned = postTransactionAmounts?.sharesOwnedFollowingTransaction.footnoted(::number),
        valueOwned = postTransactionAmounts?.valueOwnedFollowingTransaction.footnoted(::number),
        directOrIndirect = ownershipNature?.directOrIndirectOwnership.footnoted { it },
        natureOfOwnership = ownershipNature?.natureOfOwnership.footnoted { it },
        derivative = derivative,
    )

private fun DerivativeTerms.derivative() =
    Derivative(
        exercisePrice = conversionOrExercisePrice.footnoted(::number),
        exerciseDate = exerciseDate.footnoted(::parseDate),
        expirationDate = expirationDate.footnoted(::parseDate),
        underlyingTitle = underlyingSecurity?.underlyingSecurityTitle.footnoted { it },
        underlyingShares = underlyingSecurity?.underlyingSecurityShares.footnoted(::number),
        underlyingValue = underlyingSecurity?.underlyingSecurityValue.footnoted(::number),
    )

private fun TransactionCoding.toModel() =
    Coding(
        formType = transactionFormType.text()?.let(::FormType),
        value = transactionCode.text(),
        isEquitySwap = equitySwapInvolved.flag() ?: false,
        footnoteIds = footnoteId.ids(),
    )
