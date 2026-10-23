package com.alexkouzel.sec.ownership.xml

import com.fasterxml.jackson.annotation.JsonMerge

internal class NonDerivativeTable {
    @field:JsonMerge
    var nonDerivativeTransaction: MutableList<NonDerivativeTransaction> = mutableListOf()

    @field:JsonMerge
    var nonDerivativeHolding: MutableList<NonDerivativeHolding> = mutableListOf()
}

internal class DerivativeTable {
    @field:JsonMerge
    var derivativeTransaction: MutableList<DerivativeTransaction> = mutableListOf()

    @field:JsonMerge
    var derivativeHolding: MutableList<DerivativeHolding> = mutableListOf()
}

internal open class NonDerivativeTransaction {
    var securityTitle: Value? = null
    var transactionDate: Value? = null
    var deemedExecutionDate: Value? = null
    var transactionCoding: TransactionCoding? = null
    var transactionTimeliness: Value? = null
    var transactionAmounts: TransactionAmounts? = null
    var postTransactionAmounts: PostTransactionAmounts? = null
    var ownershipNature: OwnershipNature? = null
}

internal class DerivativeTransaction :
    NonDerivativeTransaction(),
    DerivativeTerms {
    override var conversionOrExercisePrice: Value? = null
    override var exerciseDate: Value? = null
    override var expirationDate: Value? = null
    override var underlyingSecurity: UnderlyingSecurity? = null
}

internal open class NonDerivativeHolding {
    var securityTitle: Value? = null
    var transactionCoding: TransactionCoding? = null
    var postTransactionAmounts: PostTransactionAmounts? = null
    var ownershipNature: OwnershipNature? = null
}

internal class DerivativeHolding :
    NonDerivativeHolding(),
    DerivativeTerms {
    override var conversionOrExercisePrice: Value? = null
    override var exerciseDate: Value? = null
    override var expirationDate: Value? = null
    override var underlyingSecurity: UnderlyingSecurity? = null
}

internal interface DerivativeTerms {
    var conversionOrExercisePrice: Value?
    var exerciseDate: Value?
    var expirationDate: Value?
    var underlyingSecurity: UnderlyingSecurity?
}

internal class TransactionCoding {
    var transactionFormType: String? = null
    var transactionCode: String? = null
    var equitySwapInvolved: String? = null

    @field:JsonMerge
    var footnoteId: MutableList<FootnoteId> = mutableListOf()
}

internal class TransactionAmounts {
    var transactionShares: Value? = null
    var transactionPricePerShare: Value? = null
    var transactionTotalValue: Value? = null
    var transactionAcquiredDisposedCode: Value? = null
}

internal class PostTransactionAmounts {
    var sharesOwnedFollowingTransaction: Value? = null
    var valueOwnedFollowingTransaction: Value? = null
}

internal class OwnershipNature {
    var directOrIndirectOwnership: Value? = null
    var natureOfOwnership: Value? = null
}

internal class UnderlyingSecurity {
    var underlyingSecurityTitle: Value? = null
    var underlyingSecurityShares: Value? = null
    var underlyingSecurityValue: Value? = null
}
