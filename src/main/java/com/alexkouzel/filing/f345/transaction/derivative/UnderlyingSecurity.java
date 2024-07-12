package com.alexkouzel.filing.f345.transaction.derivative;

import com.alexkouzel.filing.f345.footnote.FootnoteValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnderlyingSecurity {

    private FootnoteValue<String> underlyingSecurityTitle;

    private FootnoteValue<Double> underlyingSecurityShares;

    private FootnoteValue<Double> underlyingSecurityValue;

}
