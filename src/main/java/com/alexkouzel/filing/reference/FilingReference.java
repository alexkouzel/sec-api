package com.alexkouzel.filing.reference;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.FilingUrlBuilder;

import java.time.LocalDate;

public record FilingReference(

        String accNo,

        int issuerCik,

        FilingType type,

        LocalDate filedAt

) {

    public String getTxtUrl() {
        return FilingUrlBuilder.buildTxtUrl(issuerCik, accNo);
    }

}
