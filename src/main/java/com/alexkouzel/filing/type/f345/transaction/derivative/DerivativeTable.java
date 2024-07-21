package com.alexkouzel.filing.type.f345.transaction.derivative;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DerivativeTable {

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<DerivativeTransaction> derivativeTransaction;

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<DerivativeHolding> derivativeHolding;

}
