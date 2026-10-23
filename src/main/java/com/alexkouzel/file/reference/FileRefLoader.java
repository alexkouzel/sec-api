package com.alexkouzel.file.reference;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.company.CikSubmission;
import com.alexkouzel.file.reference.company.CikSubmissionFilings;
import com.alexkouzel.file.reference.company.CikSubmissionParser;
import com.alexkouzel.file.reference.daysago.DailyFeedParser;
import com.alexkouzel.file.reference.fiscalquarter.IndexFeedParser;
import com.alexkouzel.file.reference.latest.LatestFeed;
import com.alexkouzel.file.reference.latest.LatestFilesLimit;
import com.alexkouzel.file.reference.latest.LatestFeedParser;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileRefLoader {
    private static final String URL_COMPANY = "https://data.sec.gov/submissions/CIK%s.json";
    private static final String URL_COMPANY_OLDER = "https://data.sec.gov/submissions/%s";
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

    /**
     * Every filing of this CIK, newest first: {@code filings.recent} of the submissions JSON
     * (at most 1,000 filings or one year), then each older file it lists in {@code filings.files}.
     * Each older file costs one extra request.
     */
    public List<FileRef> loadCompany(int cik) throws HttpRequestException, ParsingException {
        String cikValue = StringUtils.padLeft(String.valueOf(cik), 10, '0');
        String url = String.format(URL_COMPANY, cikValue);
        CikSubmission submission = client.loadJson(url, CikSubmission.class);

        CikSubmissionFilings filings = submission.getFilings();
        List<FileRef> fileRefs = new ArrayList<>(CikSubmissionParser.parse(filings.getRecent(), submission.getCik()));
        if (filings.getFiles() != null) {
            for (CikSubmission.FilingFile file : filings.getFiles()) {
                String fileUrl = String.format(URL_COMPANY_OLDER, file.getName());
                CikSubmissionFilings.Recent older = client.loadJson(fileUrl, CikSubmissionFilings.Recent.class);
                fileRefs.addAll(CikSubmissionParser.parse(older, submission.getCik()));
            }
        }
        return fileRefs;
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

    /**
     * Filings of exactly this form type. EDGAR's own filter is a prefix match
     * (type "4" also matches "424B2"), so results are filtered again here.
     */
    public List<FileRef> loadDaysAgo(int daysAgo, FileType type) throws HttpRequestException, ParsingException {
        String url = String.format(URL_DAYS_AGO, daysAgo) + "&q3=" + encode(type.getValue());
        return FileRefUtils.filterType(loadDaysAgo(url), type);
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
    
    /**
     * Filings of exactly this form type among the latest {@code count} feed entries.
     * EDGAR's own filter is a prefix match (type "4" also matches "424B2"), so results
     * are filtered again here and may be fewer than {@code count}, or none.
     */
    public List<FileRef> loadLatest(int start, LatestFilesLimit count, FileType type) throws ParsingException, HttpRequestException {
        String url = String.format(URL_LATEST, start, count.getValue()) + "&type=" + encode(type.getValue());
        return FileRefUtils.filterType(loadLatest(url), type);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public List<FileRef> loadLatest(String url) throws ParsingException, HttpRequestException {
        LatestFeed feed = client.loadXml(url, LatestFeed.class);
        return LatestFeedParser.parse(feed);
    }
}
