package com.alexkouzel.company;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompanyLoaderTest {

    private static final CompanyLoader loader = new CompanyLoader(new TestEdgarClient());

    @Test
    public void test() throws HttpRequestException {
        List<Company> companies = loader.loadAll();
        assertTrue(companies.size() > 10_000);
        verifyCompany(companies.get(0));
    }

    private void verifyCompany(Company company) {
        assertNotNull(company);
        assertNotNull(company.id());
        assertNotNull(company.exchange());
        assertNotNull(company.name());
        assertNotNull(company.symbol());
    }

}
