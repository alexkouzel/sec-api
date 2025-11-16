package com.alexkouzel.file.type.f345.transaction;

import com.alexkouzel.common.deserializers.BooleanDeserializer;
import com.alexkouzel.file.type.f345.footnote.FootnoteEdgarDate;
import com.alexkouzel.file.type.f345.footnote.FootnoteValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transaction {
    private FootnoteValue<String> securityTitle;
    private FootnoteValue<String> transactionTimeliness;
    private PostTransactionAmounts postTransactionAmounts;
    private FootnoteEdgarDate transactionDate;
    private FootnoteEdgarDate deemedExecutionDate;
    private OwnershipNature ownershipNature;
    private Coding transactionCoding;

    @Getter
    @Setter
    public static class Coding {
        private String transactionFormType;
        @JsonDeserialize(using = TransactionCodeDeserializer.class)
        private TransactionCode transactionCode;
        @JsonDeserialize(using = BooleanDeserializer.class)
        private boolean equitySwapInvolved;
        @JacksonXmlProperty(isAttribute = true)
        private Integer footnoteId;
    }
}
