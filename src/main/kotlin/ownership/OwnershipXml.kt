package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.filing.FormType
import com.alexkouzel.sec.http.attribute
import com.alexkouzel.sec.http.child
import com.alexkouzel.sec.http.children
import com.alexkouzel.sec.http.parseXml
import com.alexkouzel.sec.http.text
import com.alexkouzel.sec.http.trimmedText
import org.w3c.dom.Element
import java.time.LocalDate

// The ownership XML (<ownershipDocument>), read element by element as the XSD in specs/ names
// them, into the model. Repeated elements may be interleaved with others; children() keeps
// them all, in document order.
internal object OwnershipXml {
    fun parse(
        content: ByteArray,
        accessionNumber: String,
        formType: FormType,
        filedOn: LocalDate,
        document: String,
    ): OwnershipFiling {
        val root = parseXml(content)
        if (root.tagName != "ownershipDocument") {
            throw SecException.Parse("Expected <ownershipDocument>, found <${root.tagName}>")
        }
        val nonDerivative = root.child("nonDerivativeTable")
        val derivative = root.child("derivativeTable")
        val nonDerivativeTransactions = nonDerivative.children("nonDerivativeTransaction")
        val nonDerivativeHoldings = nonDerivative.children("nonDerivativeHolding")
        val derivativeTransactions = derivative.children("derivativeTransaction")
        val derivativeHoldings = derivative.children("derivativeHolding")

        val transactions =
            nonDerivativeTransactions.map { transaction(it, derivative = null) } +
                derivativeTransactions.map { transaction(it, derivative(it)) }
        val holdings =
            nonDerivativeHoldings.map { holding(it, derivative = null) } +
                derivativeHoldings.map { holding(it, derivative(it)) }

        return OwnershipFiling(
            accessionNumber = accessionNumber,
            formType = formType,
            filedOn = filedOn,
            document = document,
            schemaVersion = root.text("schemaVersion"),
            periodOfReport = root.date("periodOfReport"),
            dateOfOriginalSubmission = root.date("dateOfOriginalSubmission"),
            issuer = issuer(root),
            owners = root.children("reportingOwner").map(::owner),
            transactions = transactions,
            holdings = holdings,
            footnotes = root.child("footnotes").children("footnote").map(::footnote),
            signatures = root.children("ownerSignature").map(::signature),
            remarks = root.text("remarks"),
            ownsNoSecurities = root.flag("noSecuritiesOwned") ?: false,
            isNotSubjectToSection16 = root.flag("notSubjectToSection16") ?: false,
            hasForm3HoldingsReported = root.flag("form3HoldingsReported"),
            hasForm4TransactionsReported = root.flag("form4TransactionsReported"),
            isRule10b5One = root.flag("aff10b5One"),
        )
    }

    private fun issuer(root: Element): Issuer {
        val issuer =
            root.child("issuer")
                ?: throw SecException.Parse("No <issuer> in the ownership document")
        return Issuer(
            cik = issuer.cik("issuerCik"),
            name = issuer.text("issuerName"),
            ticker = issuer.text("issuerTradingSymbol"),
            foreignTicker = issuer.text("issuerForeignTradingSymbol"),
        )
    }

    private fun owner(owner: Element): ReportingOwner {
        val id =
            owner.child("reportingOwnerId")
                ?: throw SecException.Parse("A <reportingOwner> has no id")
        val relationship = owner.child("reportingOwnerRelationship")
        return ReportingOwner(
            cik = id.cik("rptOwnerCik"),
            name = id.text("rptOwnerName"),
            address = owner.child("reportingOwnerAddress")?.let(::address),
            relationship =
                Relationship(
                    isDirector = relationship?.flag("isDirector") ?: false,
                    isOfficer = relationship?.flag("isOfficer") ?: false,
                    isTenPercentOwner = relationship?.flag("isTenPercentOwner") ?: false,
                    isOther = relationship?.flag("isOther") ?: false,
                    officerTitle = relationship?.text("officerTitle"),
                    otherText = relationship?.text("otherText"),
                ),
        )
    }

    private fun address(address: Element) =
        Address(
            street1 = address.text("rptOwnerStreet1"),
            street2 = address.text("rptOwnerStreet2"),
            city = address.text("rptOwnerCity"),
            state = address.text("rptOwnerState"),
            zipCode = address.text("rptOwnerZipCode"),
            stateDescription = address.text("rptOwnerStateDescription"),
            isNonUS = address.flag("rptOwnerNonUSAddressFlag"),
            nonUSStateTerritory = address.text("rptOwnerNonUSStateTerritory"),
            country = address.text("rptOwnerCountry"),
        )

    private fun signature(signature: Element) =
        Signature(signature.text("signatureName"), signature.date("signatureDate"))

    private fun footnote(footnote: Element): Footnote {
        val id = footnote.attribute("id") ?: throw SecException.Parse("A footnote has no id")
        return Footnote(id, footnote.trimmedText().orEmpty())
    }

    private fun transaction(
        transaction: Element,
        derivative: Derivative?,
    ): Transaction {
        val amounts = transaction.child("transactionAmounts")
        val after = transaction.child("postTransactionAmounts")
        val nature = transaction.child("ownershipNature")
        return Transaction(
            securityTitle = transaction.footnoted("securityTitle") { it },
            date = transaction.footnoted("transactionDate", ::ownershipDate),
            deemedExecutionDate = transaction.footnoted("deemedExecutionDate", ::ownershipDate),
            coding = transaction.child("transactionCoding")?.let(::coding),
            timeliness = transaction.footnoted("transactionTimeliness") { it },
            shares = amounts.footnoted("transactionShares", ::number),
            pricePerShare = amounts.footnoted("transactionPricePerShare", ::number),
            totalValue = amounts.footnoted("transactionTotalValue", ::number),
            acquiredOrDisposed = amounts.footnoted("transactionAcquiredDisposedCode") { it },
            sharesOwnedAfter = after.footnoted("sharesOwnedFollowingTransaction", ::number),
            valueOwnedAfter = after.footnoted("valueOwnedFollowingTransaction", ::number),
            directOrIndirect = nature.footnoted("directOrIndirectOwnership") { it },
            natureOfOwnership = nature.footnoted("natureOfOwnership") { it },
            derivative = derivative,
        )
    }

    private fun holding(
        holding: Element,
        derivative: Derivative?,
    ): Holding {
        val coding = holding.child("transactionCoding")
        val formType = coding?.text("transactionFormType")?.let(::FormType)
        val after = holding.child("postTransactionAmounts")
        val nature = holding.child("ownershipNature")
        return Holding(
            securityTitle = holding.footnoted("securityTitle") { it },
            formType = if (coding == null) null else footnoted(formType, coding),
            sharesOwned = after.footnoted("sharesOwnedFollowingTransaction", ::number),
            valueOwned = after.footnoted("valueOwnedFollowingTransaction", ::number),
            directOrIndirect = nature.footnoted("directOrIndirectOwnership") { it },
            natureOfOwnership = nature.footnoted("natureOfOwnership") { it },
            derivative = derivative,
        )
    }

    // The terms of a derivative transaction or holding.
    private fun derivative(row: Element): Derivative {
        val underlying = row.child("underlyingSecurity")
        return Derivative(
            exercisePrice = row.footnoted("conversionOrExercisePrice", ::number),
            exerciseDate = row.footnoted("exerciseDate", ::ownershipDate),
            expirationDate = row.footnoted("expirationDate", ::ownershipDate),
            underlyingTitle = underlying.footnoted("underlyingSecurityTitle") { it },
            underlyingShares = underlying.footnoted("underlyingSecurityShares", ::number),
            underlyingValue = underlying.footnoted("underlyingSecurityValue", ::number),
        )
    }

    private fun coding(coding: Element) =
        Coding(
            formType = coding.text("transactionFormType")?.let(::FormType),
            value = coding.text("transactionCode"),
            isEquitySwap = coding.flag("equitySwapInvolved") ?: false,
            footnoteIds = footnoteIds(coding),
        )
}
