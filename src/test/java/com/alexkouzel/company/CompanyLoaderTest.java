package com.alexkouzel.company;

import com.alexkouzel.client.EdgarClient;
import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.company.Company;
import com.alexkouzel.company.CompanyLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompanyLoaderTest {

    private static final EdgarClient client = new EdgarClient("TestCompany test@gmail.com");
    private static final CompanyLoader loader = new CompanyLoader(client);

    @Test
    public void test() throws RequestFailedException, ParsingException {
        List<Company> companies = loader.loadCompanies();
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
