package com.alexkouzel.sec.filing

import com.alexkouzel.sec.http.Http
import com.alexkouzel.sec.http.get

/**
 * Lists of filings from EDGAR's indexes and feeds. Each filing appears once, in the source's
 * order. Where a [FormType] is given, only filings of exactly that form type are returned:
 * EDGAR's own filter is a prefix match ("4" also returns "424B2"), so there may be fewer than
 * requested, or none.
 */
public class Filings internal constructor(
    private val http: Http,
) {
    /** The newest filings from the latest filings feed; [count] is 10, 20, 40, 80 or 100. */
    @JvmOverloads
    public fun latest(
        count: Int = 40,
        form: FormType? = null,
    ): List<FilingRef> {
        require(count in LatestFeed.counts) { "count must be one of ${LatestFeed.counts}: $count" }

        val refs = http.get(LatestFeed.url(count, form), LatestFeed::parse)
        return refs.ofForm(form)
    }

    /** Filings from [daysAgo] days ago (0 is today), from the daily list. */
    @JvmOverloads
    public fun daily(
        daysAgo: Int = 0,
        form: FormType? = null,
    ): List<FilingRef> {
        require(daysAgo >= 0) { "daysAgo must not be negative: $daysAgo" }

        val refs = http.get(DailyList.url(daysAgo, form), DailyList::parse)
        return refs.ofForm(form)
    }

    /**
     * Every filing in a calendar quarter, from the quarterly index (since 1993). A past
     * quarter's index is tens of megabytes.
     */
    public fun quarter(
        year: Int,
        quarter: Int,
    ): List<FilingRef> {
        require(year >= FIRST_YEAR) { "EDGAR's indexes start in $FIRST_YEAR: $year" }
        require(quarter in QUARTERS) { "quarter must be in $QUARTERS: $quarter" }

        return http.get(QuarterlyIndex.url(year, quarter), QuarterlyIndex::parse)
    }

    /**
     * Every filing related to [cik], newest first, from its submissions. Costs one request,
     * plus one per file of older filings (one per about 1,000 filings).
     */
    public fun company(cik: Long): List<FilingRef> {
        val page = http.get(Submissions.url(cik)) { Submissions.parse(it, cik) }
        val older =
            page.olderFiles.flatMap { file ->
                http.get(Submissions.olderUrl(file)) { Submissions.parseOlder(it, cik) }
            }
        return page.refs + older
    }

    private fun List<FilingRef>.ofForm(form: FormType?): List<FilingRef> {
        if (form == null) return this
        return filter { it.formType == form }
    }

    private companion object {
        const val FIRST_YEAR = 1993
        val QUARTERS = 1..4
    }
}
