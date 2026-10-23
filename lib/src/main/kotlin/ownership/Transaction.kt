package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.filing.FormType
import java.time.LocalDate

/**
 * A change in holdings. [derivative] holds the terms of a derivative security (an option, a
 * warrant, ...) and is null for the security itself. [acquiredOrDisposed] is `A` or `D`,
 * [directOrIndirect] `D` or `I`.
 */
public data class Transaction(
    val securityTitle: Footnoted<String>?,
    val date: Footnoted<LocalDate>?,
    val deemedExecutionDate: Footnoted<LocalDate>?,
    val coding: Coding?,
    val timeliness: Footnoted<String>?,
    val shares: Footnoted<Double>?,
    val pricePerShare: Footnoted<Double>?,
    val totalValue: Footnoted<Double>?,
    val acquiredOrDisposed: Footnoted<String>?,
    val sharesOwnedAfter: Footnoted<Double>?,
    val valueOwnedAfter: Footnoted<Double>?,
    val directOrIndirect: Footnoted<String>?,
    val natureOfOwnership: Footnoted<String>?,
    val derivative: Derivative?,
)

/** A position held without a reported transaction; see [Transaction] for the fields. */
public data class Holding(
    val securityTitle: Footnoted<String>?,
    val formType: Footnoted<FormType>?,
    val sharesOwned: Footnoted<Double>?,
    val valueOwned: Footnoted<Double>?,
    val directOrIndirect: Footnoted<String>?,
    val natureOfOwnership: Footnoted<String>?,
    val derivative: Derivative?,
)

public data class Derivative(
    val exercisePrice: Footnoted<Double>?,
    val exerciseDate: Footnoted<LocalDate>?,
    val expirationDate: Footnoted<LocalDate>?,
    val underlyingTitle: Footnoted<String>?,
    val underlyingShares: Footnoted<Double>?,
    val underlyingValue: Footnoted<Double>?,
)

/** [code] is null when the filing has none or one this library doesn't know; [value] keeps it. */
public data class Coding(
    val formType: FormType?,
    val value: String?,
    val isEquitySwap: Boolean,
    val footnoteIds: List<String>,
) {
    val code: TransactionCode? get() = value?.let { TransactionCode.of(it) }
}

public enum class TransactionCode(
    public val value: String,
) {
    PURCHASE("P"),
    SALE("S"),
    REPORTED_EARLIER("V"),
    SALE_BACK_TO_ISSUER("D"),
    TAX_OR_EXERCISE_PAYMENT("F"),
    GRANT_OR_AWARD("A"),
    DISCRETIONARY("I"),
    OPTION_EXERCISE("M"),
    CONVERSION("C"),
    EXPIRATION_SHORT("E"),
    EXPIRATION_LONG("H"),
    EXERCISE_OUT_OF_THE_MONEY("O"),
    EXERCISE_IN_THE_MONEY("X"),
    GIFT("G"),
    SMALL_ACQUISITION("L"),
    INHERITANCE("W"),
    VOTING_TRUST("Z"),
    OTHER("J"),
    EQUITY_SWAP("K"),
    TENDER_OF_SHARES("U"),
    ;

    public companion object {
        @JvmStatic
        public fun of(value: String): TransactionCode? = entries.find { it.value == value }
    }
}
