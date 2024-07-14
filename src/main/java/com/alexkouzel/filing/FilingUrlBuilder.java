package com.alexkouzel.filing;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class FilingUrlBuilder {

    private final String URL_TEMPLATE = "https://www.sec.gov/Archives/edgar/data/%s/%s/%s";

    private final Pattern DASH_PATTERN = Pattern.compile("-");

    public String buildTxtUrl(String issuerCik, String accNum) {
        return buildUrl(issuerCik, accNum, accNum + ".txt");
    }

    /* Only valid for ownership docs */
    public String buildXmlUrl(String issuerCik, String accNum, String filename) {
        return buildUrl(issuerCik, accNum, "xslF345X03/" + filename);
    }

    private String buildUrl(String issuerCik, String accNum, String filename) {
        String urlAccNum = DASH_PATTERN.matcher(accNum).replaceAll("");
        return String.format(URL_TEMPLATE, issuerCik, urlAccNum, filename);
    }

}
