package com.alexkouzel.file.type.f345.transaction.derivative;

import com.alexkouzel.file.type.f345.footnote.FootnoteValue;
import com.alexkouzel.file.type.f345.transaction.TransactionAmounts;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DerivativeTransactionAmounts extends TransactionAmounts {
    private FootnoteValue<Double> transactionTotalValue;
}
