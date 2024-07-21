package com.alexkouzel.filing.metadata;

import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.metadata.cik.CikSubmission;
import com.alexkouzel.filing.metadata.latest.LatestFeedCount;
import com.alexkouzel.filing.metadata.latest.LatestFeedParser;
import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.filing.metadata.cik.CikSubmissionParser;
import com.alexkouzel.filing.metadata.daily.DailyFeedParser;
import com.alexkouzel.filing.metadata.index.IndexFeedParser;
import com.alexkouzel.filing.metadata.latest.LatestFeed;

import java.io.InputStream;
import java.util.List;

public class FilingMetadataLoader {

    private static final String DAILY_FEED_URL = "https://www.sec.gov/cgi-bin/current?q1=%s";

    private static final String FULL_INDEX_URL = "https://www.sec.gov/Archives/edgar/full-index/%d/QTR%d/master.idx";

    private static final String SUBMISSIONS_URL = "https://data.sec.gov/submissions/CIK%s.json";

    private static final String LATEST_FEED_URL = "https://www.sec.gov/cgi-bin/browse-edgar" +
            "?action=getcurrent" +
            "&start=%d" +
            "&count=%d" +
            "&output=atom";

    private final HttpDataClient client;

    public FilingMetadataLoader(HttpDataClient client) {
        this.client = client;
    }

    public List<FilingMetadata> loadDaysAgo(int daysAgo) throws HttpRequestException, ParsingException {
        String url = String.format(DAILY_FEED_URL, daysAgo);
        return loadDaysAgo(url);
    }

    public List<FilingMetadata> loadDaysAgo(int daysAgo, FilingType type) throws HttpRequestException, ParsingException {
        String url = String.format(DAILY_FEED_URL, daysAgo) + "&q3=" + type.getValue();
        return loadDaysAgo(url);
    }

    private List<FilingMetadata> loadDaysAgo(String url) throws HttpRequestException, ParsingException {
        String data = client.loadText(url);
        return DailyFeedParser.parse(data);
    }

    public List<FilingMetadata> loadByQuarter(int year, int quarter) throws ParsingException, HttpRequestException {
        String url = String.format(FULL_INDEX_URL, year, quarter);
        InputStream stream = client.loadStream(url, "text/html");
        return IndexFeedParser.parse(stream);
    }

    public List<FilingMetadata> loadLatest(int start, LatestFeedCount count) throws ParsingException, HttpRequestException {
        String url = String.format(LATEST_FEED_URL, start, count.getValue());
        return loadLatest(url);
    }

    public List<FilingMetadata> loadLatest(int start, LatestFeedCount count, FilingType type) throws ParsingException, HttpRequestException {
        String url = String.format(LATEST_FEED_URL, start, count.getValue()) + "&type=" + type.getValue();
        return loadLatest(url);
    }

    public List<FilingMetadata> loadLatest(String url) throws ParsingException, HttpRequestException {
        LatestFeed feed = client.loadXml(url, LatestFeed.class);
        return LatestFeedParser.parse(feed);
    }

    public List<FilingMetadata> loadByCik(String cik) throws HttpRequestException {
        cik = StringUtils.padLeft(cik, 10, '0');
        String url = String.format(SUBMISSIONS_URL, cik);
        CikSubmission submission = client.loadJson(url, CikSubmission.class);
        return CikSubmissionParser.parse(submission);
    }

}
