package com.alexkouzel.filing.type.f345.transaction.nonderivative;

import com.alexkouzel.filing.type.f345.transaction.Transaction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NonDerivativeTransaction extends Transaction {
    private NonDerivativeTransactionAmounts transactionAmounts;
}
