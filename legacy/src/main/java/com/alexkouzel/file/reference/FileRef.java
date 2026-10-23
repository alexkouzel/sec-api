package com.alexkouzel.file.reference;

import com.alexkouzel.file.FileType;
import com.alexkouzel.file.FileUrlBuilder;

import java.time.LocalDate;

public record FileRef(
        String accNo,
        int issuerCik,
        FileType type,
        LocalDate filedAt
) {
    public String buildUrl() {
        return FileUrlBuilder.buildTxt(issuerCik, accNo);
    }
}
