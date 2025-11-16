package com.alexkouzel.file.reference;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.company.CikSubmission;
import com.alexkouzel.file.reference.company.CikSubmissionParser;
import com.alexkouzel.file.reference.daysago.DailyFeedParser;
import com.alexkouzel.file.reference.fiscalquarter.IndexFeedParser;
import com.alexkouzel.file.reference.latest.LatestFeed;
import com.alexkouzel.file.reference.latest.LatestFilesLimit;
import com.alexkouzel.file.reference.latest.LatestFeedParser;

import java.io.InputStream;
import java.util.List;

public class FileRefLoader {
    private static final String URL_COMPANY = "https://data.sec.gov/submissions/CIK%s.json";
    private static final String URL_DAYS_AGO = "https://www.sec.gov/cgi-bin/current?q1=%s";
    private static final String URL_FISCAL_QUARTER = "https://www.sec.gov/Archives/edgar/full-index/%d/QTR%d/master.idx";
    private static final String URL_LATEST = "https://www.sec.gov/cgi-bin/browse-edgar" +
            "?action=getcurrent" +
            "&start=%d" +
            "&count=%d" +
            "&output=atom";

    private final HttpDataClient client;

    public FileRefLoader(HttpDataClient client) {
        this.client = client;
    }

    public List<FileRef> loadCompany(int cik) throws HttpRequestException {
        String cikValue = StringUtils.padLeft(String.valueOf(cik), 10, '0');
        String url = String.format(URL_COMPANY, cikValue);
        CikSubmission submission = client.loadJson(url, CikSubmission.class);
        return CikSubmissionParser.parse(submission);
    }

    public List<FileRef> loadLatest(int start, LatestFilesLimit count) throws ParsingException, HttpRequestException {
        String url = String.format(URL_LATEST, start, count.getValue());
        return loadLatest(url);
    }
    
    public List<FileRef> loadToday() throws HttpRequestException, ParsingException {
        return loadDaysAgo(0);
    }

    public List<FileRef> loadDaysAgo(int daysAgo) throws HttpRequestException, ParsingException {
        String url = String.format(URL_DAYS_AGO, daysAgo);
        return loadDaysAgo(url);
    }

    public List<FileRef> loadDaysAgo(int daysAgo, FileType type) throws HttpRequestException, ParsingException {
        String url = String.format(URL_DAYS_AGO, daysAgo) + "&q3=" + type.getValue();
        return loadDaysAgo(url);
    }

    private List<FileRef> loadDaysAgo(String url) throws HttpRequestException, ParsingException {
        String data = client.loadText(url);
        return DailyFeedParser.parse(data);
    }

    public List<FileRef> loadFiscalQuarter(int year, int quarter) throws ParsingException, HttpRequestException {
        String url = String.format(URL_FISCAL_QUARTER, year, quarter);
        InputStream stream = client.loadStream(url, "text/html");
        return IndexFeedParser.parse(stream);
    }

    public List<FileRef> loadLatest(LatestFilesLimit count, FileType type) throws ParsingException, HttpRequestException {
        return loadLatest(0, count, type);
    }
    
    public List<FileRef> loadLatest(int start, LatestFilesLimit count, FileType type) throws ParsingException, HttpRequestException {
        String url = String.format(URL_LATEST, start, count.getValue()) + "&type=" + type.getValue();
        return loadLatest(url);
    }

    public List<FileRef> loadLatest(String url) throws ParsingException, HttpRequestException {
        LatestFeed feed = client.loadXml(url, LatestFeed.class);
        return LatestFeedParser.parse(feed);
    }
}
