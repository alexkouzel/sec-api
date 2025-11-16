package com.alexkouzel.company;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecCompanyLoaderTest {
    private static final SecCompanyLoader loader = new SecCompanyLoader(new TestEdgarClient());

    @Test
    public void test() throws HttpRequestException {
        List<SecCompany> companies = loader.load();
        assertTrue(companies.size() > 10_000);
        verifyCompany(companies.getFirst());
    }

    private void verifyCompany(SecCompany company) {
        assertNotNull(company);
        assertNotNull(company.id());
        assertNotNull(company.exchange());
        assertNotNull(company.name());
        assertNotNull(company.ticker());
    }
}
