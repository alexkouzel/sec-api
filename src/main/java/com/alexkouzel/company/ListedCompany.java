package com.alexkouzel.company;

public record ListedCompany(
        int id,
        String name,
        String ticker,
        String exchange
) {}
