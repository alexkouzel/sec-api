package com.alexkouzel.file.type.f345.transaction;

import com.alexkouzel.file.type.f345.footnote.FootnoteValue;
import com.alexkouzel.file.type.f345.footnote.FootnoteID;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "footnoteId")
        @JsonMerge
        private List<FootnoteID> footnoteIds;
    }
}
