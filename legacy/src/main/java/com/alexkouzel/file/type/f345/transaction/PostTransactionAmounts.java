package com.alexkouzel.file.type.f345.transaction;

import com.alexkouzel.file.type.f345.footnote.FootnoteValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostTransactionAmounts {
    private FootnoteValue<Double> sharesOwnedFollowingTransaction;
    private FootnoteValue<Double> valueOwnedFollowingTransaction;
}
