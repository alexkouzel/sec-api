package com.alexkouzel.file;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class FileUrlBuilder {
    private final String URL_TEMPLATE = "https://www.sec.gov/Archives/edgar/data/%d/%s/%s";
    private final Pattern DASH_PATTERN = Pattern.compile("-");

    public String buildTxt(int issuerCik, String accNo) {
        return build(issuerCik, accNo, accNo + ".txt");
    }

    public String buildF345(int issuerCik, String accNo, String filename) {
        return build(issuerCik, accNo, "xslF345X03/" + filename);
    }

    private String build(int issuerCik, String accNo, String filename) {
        accNo = DASH_PATTERN.matcher(accNo).replaceAll("");
        return String.format(URL_TEMPLATE, issuerCik, accNo, filename);
    }
}
