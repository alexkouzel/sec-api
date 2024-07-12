package com.alexkouzel.filing.f345.transaction.derivative;

import com.alexkouzel.filing.f345.footnote.FootnoteEdgarDate;
import com.alexkouzel.filing.f345.footnote.FootnoteValue;
import com.alexkouzel.filing.f345.transaction.Transaction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DerivativeTransaction extends Transaction {

    private DerivativeTransactionAmounts transactionAmounts;

    private FootnoteValue<Double> conversionOrExercisePrice;

    private FootnoteEdgarDate exerciseDate;

    private FootnoteEdgarDate expirationDate;

    private UnderlyingSecurity underlyingSecurity;

}
