package com.alexkouzel.company;

import com.alexkouzel.testing.FixtureClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecCompanyLoaderTest {
    private static final String URL = "https://www.sec.gov/files/company_tickers_exchange.json";

    private final FixtureClient client = new FixtureClient()
            .route(URL, "files/company_tickers_exchange.json");

    private final SecCompanyLoader loader = new SecCompanyLoader(client);

    @Test
    public void loadsEveryRowInOrder() throws Exception {
        List<SecCompany> companies = loader.load();

        assertEquals(25, companies.size());
        assertEquals(new SecCompany(1045810, "NVIDIA CORP", "NVDA", "Nasdaq"), companies.getFirst());
        assertEquals(new SecCompany(320193, "Apple Inc.", "AAPL", "Nasdaq"), companies.get(1));
        assertEquals(new SecCompany(858877, "CISCO SYSTEMS, INC.", "CSCO", "Nasdaq"), companies.getLast());
    }
}
