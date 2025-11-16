package com.alexkouzel.company;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.client.exceptions.HttpRequestException;

import java.util.List;
import java.util.stream.Collectors;

public class SecCompanyLoader {
    private static final String URL = "https://www.sec.gov/files/company_tickers_exchange.json";

    private final HttpDataClient client;

    public SecCompanyLoader(HttpDataClient client) {
        this.client = client;
    }

    public List<SecCompany> load() throws HttpRequestException {
        CompanyTickersExchange data = client.loadJson(URL, CompanyTickersExchange.class);
        return data.data()
                .stream()
                .map(this::extract)
                .collect(Collectors.toList());
    }

    private record CompanyTickersExchange(
            // Should be equal to ["cik", "name", "ticker", "exchange"]
            String[] fields,
            // Data follows the format as specified in the 'fields'.
            List<String[]> data
    ) {}

    private SecCompany extract(String[] fields) {
        int id = Integer.parseInt(fields[0]);
        String name = fields[1];
        String ticker = fields[2];
        String exchange = fields[3];
        return new SecCompany(id, name, ticker, exchange);
    }
}
