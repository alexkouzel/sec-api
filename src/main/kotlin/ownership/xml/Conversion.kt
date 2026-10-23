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
): OwnershipFiling {
    val issuer = issuer ?: throw SecException.Parse("No <issuer> in the ownership document")
    val nonDerivativeTransactions = nonDerivativeTable?.nonDerivativeTransaction.orEmpty()
    val nonDerivativeHoldings = nonDerivativeTable?.nonDerivativeHolding.orEmpty()
    val derivativeTransactions = derivativeTable?.derivativeTransaction.orEmpty()
    val derivativeHoldings = derivativeTable?.derivativeHolding.orEmpty()

    val transactions =
        nonDerivativeTransactions.map { it.toModel(derivative = null) } +
            derivativeTransactions.map { it.toModel(it.derivative()) }
    val holdings =
        nonDerivativeHoldings.map { it.toModel(derivative = null) } +
            derivativeHoldings.map { it.toModel(it.derivative()) }

    return OwnershipFiling(
        accessionNumber = accessionNumber,
        formType = formType,
        filedOn = filedOn,
        document = document,
        schemaVersion = schemaVersion.text(),
        periodOfReport = periodOfReport.date(),
        dateOfOriginalSubmission = dateOfOriginalSubmission.date(),
        issuer = issuer.toModel(),
        owners = reportingOwner.map { it.toModel() },
        transactions = transactions,
        holdings = holdings,
        footnotes = footnotes?.footnote.orEmpty().map { it.toModel() },
        signatures = ownerSignature.map { it.toModel() },
        remarks = remarks.text(),
        ownsNoSecurities = noSecuritiesOwned.flag() ?: false,
        isNotSubjectToSection16 = notSubjectToSection16.flag() ?: false,
        hasForm3HoldingsReported = form3HoldingsReported.flag(),
        hasForm4TransactionsReported = form4TransactionsReported.flag(),
        isRule10b5One = aff10b5One.flag(),
    )
}

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

private fun OwnerSignature.toModel() = Signature(signatureName.text(), signatureDate.date())

private fun Footnote.toModel() =
    FootnoteModel(
        id = id.text() ?: throw SecException.Parse("A footnote has no id"),
        text = text.text().orEmpty(),
    )

private fun NonDerivativeTransaction.toModel(derivative: Derivative?): Transaction {
    val amounts = transactionAmounts
    val after = postTransactionAmounts
    val nature = ownershipNature
    return Transaction(
        securityTitle = securityTitle.footnoted { it },
        date = transactionDate.footnoted(::parseDate),
        deemedExecutionDate = deemedExecutionDate.footnoted(::parseDate),
        coding = transactionCoding?.toModel(),
        timeliness = transactionTimeliness.footnoted { it },
        shares = amounts?.transactionShares.footnoted(::number),
        pricePerShare = amounts?.transactionPricePerShare.footnoted(::number),
        totalValue = amounts?.transactionTotalValue.footnoted(::number),
        acquiredOrDisposed = amounts?.transactionAcquiredDisposedCode.footnoted { it },
        sharesOwnedAfter = after?.sharesOwnedFollowingTransaction.footnoted(::number),
        valueOwnedAfter = after?.valueOwnedFollowingTransaction.footnoted(::number),
        directOrIndirect = nature?.directOrIndirectOwnership.footnoted { it },
        natureOfOwnership = nature?.natureOfOwnership.footnoted { it },
        derivative = derivative,
    )
}

private fun NonDerivativeHolding.toModel(derivative: Derivative?): Holding {
    val coding = transactionCoding
    val formType = coding?.transactionFormType.text()?.let(::FormType)
    val after = postTransactionAmounts
    val nature = ownershipNature
    return Holding(
        securityTitle = securityTitle.footnoted { it },
        formType = if (coding == null) null else footnoted(formType, coding.footnoteId),
        sharesOwned = after?.sharesOwnedFollowingTransaction.footnoted(::number),
        valueOwned = after?.valueOwnedFollowingTransaction.footnoted(::number),
        directOrIndirect = nature?.directOrIndirectOwnership.footnoted { it },
        natureOfOwnership = nature?.natureOfOwnership.footnoted { it },
        derivative = derivative,
    )
}

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
