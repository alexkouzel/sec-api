package com.alexkouzel.filing.f345.transaction;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.alexkouzel.filing.f345.footnote.FootnoteValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Holding {

    private FootnoteValue<String> securityTitle;

    private PostTransactionAmounts postTransactionAmounts;

    private OwnershipNature ownershipNature;

    private TransactionCoding transactionCoding;

    @Getter
    @Setter
    public static class TransactionCoding {

        private String transactionFormType;

        @JacksonXmlProperty(isAttribute = true)
        int footnoteId;

    }

}
