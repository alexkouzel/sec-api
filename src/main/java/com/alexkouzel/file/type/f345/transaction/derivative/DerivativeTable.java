package com.alexkouzel.file.type.f345.transaction.derivative;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DerivativeTable {
    // @JsonMerge: repeated elements may be interleaved with others (valid per XSD);
    // without it, each later group replaces the earlier ones.
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonMerge
    private List<DerivativeTransaction> derivativeTransaction;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonMerge
    private List<DerivativeHolding> derivativeHolding;
}
