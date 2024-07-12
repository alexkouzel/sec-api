package com.alexkouzel.filing.refs;

import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.refs.cik.CikSubmission;
import com.alexkouzel.filing.refs.latest.LatestFeedParser;
import com.alexkouzel.common.utils.ListUtils;
import com.alexkouzel.client.DataClient;
import com.alexkouzel.filing.refs.cik.CikSubmissionParser;
import com.alexkouzel.filing.refs.daily.DailyFeedParser;
import com.alexkouzel.filing.refs.index.IndexFeedParser;
import com.alexkouzel.filing.refs.latest.LatestFeed;

import java.io.InputStream;
import java.util.List;

public class FilingRefLoader {

    private static final String DAILY_FEED_URL = "https://www.sec.gov/cgi-bin/current?q1=%d&q3=%s";

    private static final String FULL_INDEX_URL = "https://www.sec.gov/Archives/edgar/full-index/%d/QTR%d/master.idx";

    private static final String SUBMISSIONS_URL = "https://data.sec.gov/submissions/CIK%s.json";

    // e.g. https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=4&start=0&count=200&output=atom
    private static final String LATEST_FEED_URL =
            "https://www.sec.gov/cgi-bin/browse-edgar" +
                    "?action=getcurrent" +
                    "&type=%s" +
                    "&start=%d" +
                    "&count=%d" +
                    "&output=atom";

    private final DataClient client;

    public FilingRefLoader(DataClient client) {
        this.client = client;
    }

    public List<FilingRef> loadDaysAgo(FilingType filingType, int daysAgo)
            throws RequestFailedException, ParsingException {
        String url = String.format(DAILY_FEED_URL, daysAgo, filingType.getValue());
        String txt = client.loadText(url);
        List<FilingRef> refs = DailyFeedParser.parse(txt);
        return filterByType(filingType, refs);
    }

    public List<FilingRef> loadByQuarter(FilingType filingType, int year, int quarter)
            throws RequestFailedException, ParsingException {
        String url = String.format(FULL_INDEX_URL, year, quarter);
        InputStream stream = client.loadStream(url, "text/html");
        List<FilingRef> refs = IndexFeedParser.parse(stream);
        return filterByType(filingType, refs);
    }

    // possible 'count': 10, 20, 40, 80, 100
    public List<FilingRef> loadLatest(FilingType filingType, int start, int count)
            throws RequestFailedException, ParsingException {
        String url = String.format(LATEST_FEED_URL, filingType.getValue(), start, count);
        LatestFeed feed = client.loadXml(url, LatestFeed.class);
        List<FilingRef> refs = LatestFeedParser.parse(feed);
        return filterByType(filingType, refs);
    }

    public List<FilingRef> loadByCik(FilingType filingType, String cik)
            throws RequestFailedException, ParsingException {
        String url = String.format(SUBMISSIONS_URL, cik);
        CikSubmission submission = client.loadJson(url, CikSubmission.class);
        List<FilingRef> refs = CikSubmissionParser.parse(submission);
        return filterByType(filingType, refs);
    }

    private List<FilingRef> filterByType(FilingType filingType, List<FilingRef> refs) {
        return ListUtils.filter(refs, ref -> ref.type() == filingType);
    }

}
