package com.alexkouzel.client;

/**
 * Client for live tests. Reads the User-Agent from env {@code EDGAR_USER_AGENT}
 * ({@code Name contact@domain}, as EDGAR requires), with a fallback for local runs.
 */
public class TestEdgarClient extends EdgarClient {
    private static final String DEFAULT_USER_AGENT = "TestCompany test.company@gmail.com";

    public TestEdgarClient() {
        super(userAgent());
    }

    private static String userAgent() {
        String userAgent = System.getenv("EDGAR_USER_AGENT");
        return userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent;
    }
}
