package com.alexkouzel.company;

import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.client.DataClient;

import java.util.List;
import java.util.stream.Collectors;

public class CompanyLoader {

    private static final String URL = "https://www.sec.gov/files/company_tickers_exchange.json";

    private final DataClient client;

    public CompanyLoader(DataClient client) {
        this.client = client;
    }

    public List<Company> loadCompanies() throws ParsingException, RequestFailedException {
        CompanyTickersExchange data = client.loadJson(URL, CompanyTickersExchange.class);

        return data
                .data()
                .stream()
                .map(this::extractCompany)
                .collect(Collectors.toList());
    }

    private record CompanyTickersExchange(

            // Should be equal to ["cik", "name", "ticker", "exchange"]
            String[] fields,

            // Data follows the format as specified in the 'fields'.
            List<String[]> data

    ) {}

    private Company extractCompany(String[] fields) {
        String id = StringUtils.padLeft(fields[0], 10, '0');
        String name = fields[1];
        String symbol = fields[2];
        String exchange = fields[3];
        return new Company(id, name, symbol, exchange);
    }

}
