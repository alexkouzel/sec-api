package com.alexkouzel.company;

import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.client.HttpDataClient;

import java.util.List;
import java.util.stream.Collectors;

public class ListedCompanyLoader {

    private static final String URL = "https://www.sec.gov/files/company_tickers_exchange.json";

    private final HttpDataClient client;

    public ListedCompanyLoader(HttpDataClient client) {
        this.client = client;
    }

    public List<ListedCompany> loadAll() throws HttpRequestException {
        CompanyTickersExchange data = client.loadJson(URL, CompanyTickersExchange.class);

        return data
                .data()
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

    private ListedCompany extract(String[] fields) {
        return new ListedCompany(fields[0], fields[1], fields[2], fields[3]);
    }

}