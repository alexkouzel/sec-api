package com.alexkouzel.sec.company

import com.alexkouzel.sec.http.Http
import com.alexkouzel.sec.http.get

public class Companies internal constructor(
    private val http: Http,
) {
    /**
     * Every company with a ticker, in EDGAR's order. A company with several tickers appears once
     * per ticker.
     */
    public fun all(): List<Company> = http.get(Tickers.URL, Tickers::parse)
}
