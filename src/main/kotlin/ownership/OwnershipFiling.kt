package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.filing.FormType
import java.time.LocalDate

/**
 * A Form 3, 4 or 5 filing, or an amendment of one. [document] is the name of its XML document.
 * Transactions and holdings of both tables are in [transactions] and [holdings], in filing
 * order, non-derivative first.
 */
public data class OwnershipFiling(
    val accessionNumber: String,
    val formType: FormType,
    val filedOn: LocalDate,
    val document: String,
    val schemaVersion: String?,
    val periodOfReport: LocalDate?,
    val dateOfOriginalSubmission: LocalDate?,
    val issuer: Issuer,
    val owners: List<ReportingOwner>,
    val transactions: List<Transaction>,
    val holdings: List<Holding>,
    val footnotes: List<Footnote>,
    val signatures: List<Signature>,
    val remarks: String?,
    /** Form 3: the owner holds no securities of the issuer. */
    val ownsNoSecurities: Boolean,
    val isNotSubjectToSection16: Boolean,
    /** Form 5 only. */
    val hasForm3HoldingsReported: Boolean?,
    /** Form 5 only. */
    val hasForm4TransactionsReported: Boolean?,
    /** Whether a transaction was made under a Rule 10b5-1 plan; null before schema X0508. */
    val isRule10b5One: Boolean?,
)
