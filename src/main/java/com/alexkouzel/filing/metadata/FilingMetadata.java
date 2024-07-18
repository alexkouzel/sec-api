package com.alexkouzel.filing.metadata;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.FilingUrlBuilder;

import java.time.LocalDate;

public record FilingMetadata(

        String accNo,

        String issuerCik,

        FilingType type,

        LocalDate filedAt

) {

    public String getTxtUrl() {
        return FilingUrlBuilder.buildTxtUrl(issuerCik, accNo);
    }

}
