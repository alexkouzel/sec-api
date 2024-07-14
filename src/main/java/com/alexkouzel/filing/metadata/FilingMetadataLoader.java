package com.alexkouzel.filing.metadata;

import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.metadata.cik.CikSubmission;
import com.alexkouzel.filing.metadata.latest.LatestFeedParser;
import com.alexkouzel.common.utils.ListUtils;
import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.filing.metadata.cik.CikSubmissionParser;
import com.alexkouzel.filing.metadata.daily.DailyFeedParser;
import com.alexkouzel.filing.metadata.index.IndexFeedParser;
import com.alexkouzel.filing.metadata.latest.LatestFeed;

import java.io.InputStream;
import java.util.List;

public class FilingMetadataLoader {

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

    private final HttpDataClient client;

    public FilingMetadataLoader(HttpDataClient client) {
        this.client = client;
    }

    public List<FilingMetadata> loadDaysAgo(FilingType filingType, int daysAgo)
            throws HttpRequestException, ParsingException {
        String url = String.format(DAILY_FEED_URL, daysAgo, filingType.getValue());
        String txt = client.loadText(url);
        List<FilingMetadata> metadata = DailyFeedParser.parse(txt);
        return filterByType(filingType, metadata);
    }

    public List<FilingMetadata> loadByQuarter(FilingType type, int year, int quarter)
            throws HttpRequestException, ParsingException {
        String url = String.format(FULL_INDEX_URL, year, quarter);
        InputStream stream = client.loadStream(url, "text/html");
        List<FilingMetadata> metadata = IndexFeedParser.parse(stream);
        return filterByType(type, metadata);
    }

    // possible 'count': 10, 20, 40, 80, 100
    public List<FilingMetadata> loadLatest(FilingType type, int start, int count)
            throws HttpRequestException, ParsingException {
        String url = String.format(LATEST_FEED_URL, type.getValue(), start, count);
        LatestFeed feed = client.loadXml(url, LatestFeed.class);
        List<FilingMetadata> metadata = LatestFeedParser.parse(feed);
        return filterByType(type, metadata);
    }

    public List<FilingMetadata> loadByCik(FilingType type, String cik) throws HttpRequestException {
        String url = String.format(SUBMISSIONS_URL, cik);
        CikSubmission submission = client.loadJson(url, CikSubmission.class);
        List<FilingMetadata> metadata = CikSubmissionParser.parse(submission);
        return filterByType(type, metadata);
    }

    private List<FilingMetadata> filterByType(FilingType filingType, List<FilingMetadata> metadata) {
        return ListUtils.filter(metadata, m -> m.type() == filingType);
    }

}
