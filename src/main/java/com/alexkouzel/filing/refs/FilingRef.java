package com.alexkouzel.filing.refs;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.FilingUrlParser;

import java.time.LocalDate;

public record FilingRef(

        String accNum,

        String issuerCik,

        FilingType type,

        LocalDate filedAt

) {

    public String getTxtUrl() {
        return FilingUrlParser.getTxtUrl(issuerCik, accNum);
    }

}
