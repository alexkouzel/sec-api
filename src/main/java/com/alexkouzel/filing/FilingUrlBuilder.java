package com.alexkouzel.filing;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class FilingUrlBuilder {

    private final String URL_TEMPLATE = "https://www.sec.gov/Archives/edgar/data/%d/%s/%s";

    private final Pattern DASH_PATTERN = Pattern.compile("-");

    public String buildTxtUrl(int issuerCik, String accNo) {
        return buildUrl(issuerCik, accNo, accNo + ".txt");
    }

    /* Only valid for ownership docs */
    public String buildXmlUrl(int issuerCik, String accNo, String filename) {
        return buildUrl(issuerCik, accNo, "xslF345X03/" + filename);
    }

    private String buildUrl(int issuerCik, String accNo, String filename) {
        accNo = DASH_PATTERN.matcher(accNo).replaceAll("");
        return String.format(URL_TEMPLATE, issuerCik, accNo, filename);
    }

}
