package com.alexkouzel.filing.type.f345.transaction;

import com.alexkouzel.filing.type.f345.footnote.FootnoteValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionAmounts {
    private FootnoteValue<Double> transactionShares;
    private FootnoteValue<Double> transactionPricePerShare;
    private FootnoteValue<String> transactionAcquiredDisposedCode;
}
