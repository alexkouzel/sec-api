package com.alexkouzel.filing.metadata;

import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.FilingUrlBuilder;

import java.time.LocalDate;

public record FilingMetadata(

        String accNum,

        String issuerCik,

        FilingType type,

        LocalDate filedAt

) {

    public String getTxtUrl() {
        return FilingUrlBuilder.buildTxtUrl(issuerCik, accNum);
    }

}
