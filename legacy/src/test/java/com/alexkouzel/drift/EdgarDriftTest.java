package com.alexkouzel.drift;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import com.alexkouzel.file.reference.company.CikSubmission;
import com.alexkouzel.file.reference.company.CikSubmissionFilings;
import com.alexkouzel.file.reference.company.CikSubmissionParser;
import com.alexkouzel.file.reference.daysago.DailyFeedParser;
import com.alexkouzel.file.reference.fiscalquarter.IndexFeedParser;
import com.alexkouzel.file.reference.latest.LatestFeed;
import com.alexkouzel.file.reference.latest.LatestFeedParser;
import com.alexkouzel.file.type.f345.OwnershipDocument;
import com.alexkouzel.file.type.f345.OwnershipDocumentParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Samples recent data from every EDGAR source and parses it strictly, collecting
 * every difference from the model instead of stopping at the first. Writes
 * build/drift/report.md and saves each offending response under build/drift/raw/.
 * Run with ./gradlew driftCheck; see "Spec drift monitoring" in docs/ROADMAP.md.
 */
@Tag("drift")
public class EdgarDriftTest {
    private static final Path OUT = Path.of(System.getProperty("drift.dir", "build/drift"));

    private static final String URL_TICKERS = "https://www.sec.gov/files/company_tickers_exchange.json";
    private static final String URL_SUBMISSIONS = "https://data.sec.gov/submissions/%s";
    private static final String URL_LATEST = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&start=0&count=40&output=atom";
    private static final String URL_DAILY = "https://www.sec.gov/cgi-bin/current?q1=1";
    private static final String URL_INDEX = "https://www.sec.gov/Archives/edgar/full-index/%d/QTR%d/master.idx";

    private static final List<String> TICKER_FIELDS = List.of("cik", "name", "ticker", "exchange");
    private static final String INDEX_COLUMNS = "CIK|Company Name|Form Type|Date Filed|Filename";

    // Apple, Tesla, the fixture's CIK (all with older filings files), and an insider (Tim Cook).
    private static final List<Integer> SUBMISSION_CIKS = List.of(320193, 1318605, 946581, 1214156);

    // About 50 recent ownership filings, every form type.
    private static final Map<FileType, Integer> OWNERSHIP_SAMPLE = new LinkedHashMap<>();

    static {
        OWNERSHIP_SAMPLE.put(FileType.F4, 20);
        OWNERSHIP_SAMPLE.put(FileType.F3, 10);
        OWNERSHIP_SAMPLE.put(FileType.F5, 6);
        OWNERSHIP_SAMPLE.put(FileType.F4_A, 6);
        OWNERSHIP_SAMPLE.put(FileType.F3_A, 4);
        OWNERSHIP_SAMPLE.put(FileType.F5_A, 4);
    }

    private final UnknownFieldCollector unknown = new UnknownFieldCollector();
    private final ObjectMapper json = unknown.jsonMapper();
    private final XmlMapper xml = unknown.xmlMapper();
    private final DriftReport report = new DriftReport(OUT, LocalDate.now(ZoneOffset.UTC));

    private KnownVersions known;
    private TestEdgarClient client;

    private interface Parse<T> {
        T run() throws Exception;
    }

    @Test
    public void edgarMatchesModel() throws Exception {
        known = KnownVersions.load();
        try (TestEdgarClient edgarClient = new TestEdgarClient()) {
            client = edgarClient;
            specPage();
            companyTickers();
            for (int cik : SUBMISSION_CIKS) {
                submissions(cik);
            }
            latestFeed("Latest feed", URL_LATEST, "latest/getcurrent-count-40.atom");
            latestFeed("Latest feed (type=4)", URL_LATEST + "&type=4", "latest/getcurrent-count-40-type-4.atom");
            dailyList("Daily list", URL_DAILY, "current/current-q1-1.html");
            dailyList("Daily list (q3=4)", URL_DAILY + "&q3=4", "current/current-q1-1-q3-4.html");
            ownershipFilings(quarterlyIndexes());
        } finally {
            report.write();
        }
        assertEquals(List.of(), report.findings(), "EDGAR drift, see " + OUT.resolve("report.md"));
    }

    private void specPage() throws IOException {
        String source = "Spec page";
        byte[] raw = fetch(source, SpecWatcher.URL);
        if (raw == null) {
            return;
        }
        String html = text(raw);
        Optional<String> current = SpecWatcher.currentVersion(html);
        Set<String> linked = SpecWatcher.linkedVersions(html);
        report.checked(source, "Ownership section says version " + current.orElse("?")
                + ", packages linked: " + String.join(", ", linked) + "; `specs/` has " + known.ownershipSpec());

        if (current.isEmpty()) {
            report.finding(source, "layout changed", "No version found in the Ownership section",
                    "spec-page/technical-specifications.html", raw);
        } else if (!current.get().equals(known.ownershipSpec())) {
            report.finding(source, "new spec version", "Ownership spec " + current.get()
                    + " is current; `specs/` has " + known.ownershipSpec());
        }
        for (String version : linked) {
            if (SpecWatcher.compareVersions(version, known.ownershipSpec()) > 0 && !version.equals(current.orElse(null))) {
                report.finding(source, "new spec version", "Ownership spec package " + version + " is linked");
            }
        }
    }

    private void companyTickers() throws IOException {
        String source = "Company tickers";
        String rawName = "files/company_tickers_exchange.json";
        byte[] raw = fetch(source, URL_TICKERS);
        if (raw == null) {
            return;
        }
        JsonNode root = parse(source, rawName, raw, () -> json.readTree(raw));
        if (root == null) {
            return;
        }
        List<String> fields = new ArrayList<>();
        root.path("fields").forEach(field -> fields.add(field.asText()));
        if (!fields.equals(TICKER_FIELDS)) {
            report.finding(source, "format changed", "fields are " + fields + ", expected " + TICKER_FIELDS, rawName, raw);
        }
        Set<String> extra = new TreeSet<>();
        root.fieldNames().forEachRemaining(extra::add);
        extra.removeAll(Set.of("fields", "data"));
        if (!extra.isEmpty()) {
            report.finding(source, "unknown fields", String.join(", ", extra), rawName, raw);
        }
        int rows = root.path("data").size();
        long badRows = 0;
        for (JsonNode row : root.path("data")) {
            if (row.size() != TICKER_FIELDS.size() || !row.get(0).canConvertToInt()) {
                badRows++;
            }
        }
        if (rows == 0 || badRows > 0) {
            report.finding(source, "format changed", rows + " rows, " + badRows + " not [cik, name, ticker, exchange]", rawName, raw);
        }
        report.checked(source, rows + " rows");
    }

    private void submissions(int cik) throws IOException {
        String name = String.format("CIK%010d.json", cik);
        String source = "Submissions " + name;
        byte[] raw = fetch(source, String.format(URL_SUBMISSIONS, name));
        if (raw == null) {
            return;
        }
        CikSubmission submission = parse(source, "submissions/" + name, raw, () -> {
            CikSubmission parsed = json.readValue(raw, CikSubmission.class);
            parseFilings(parsed.getFilings().getRecent(), cik);
            return parsed;
        });
        if (submission == null) {
            return;
        }
        int recent = submission.getFilings().getRecent().getAccessionNumber().size();
        List<CikSubmission.FilingFile> files = submission.getFilings().getFiles();
        if (files == null || files.isEmpty()) {
            report.checked(source, recent + " recent filings");
            return;
        }
        // The newest older file is enough to see its format.
        String olderName = files.getFirst().getName();
        byte[] olderRaw = fetch(source, String.format(URL_SUBMISSIONS, olderName));
        if (olderRaw != null) {
            parse(source, "submissions/" + olderName, olderRaw,
                    () -> parseFilings(json.readValue(olderRaw, CikSubmissionFilings.Recent.class), cik));
        }
        report.checked(source, recent + " recent filings, older file " + olderName + " of " + files.size());
    }

    private static List<FileRef> parseFilings(CikSubmissionFilings.Recent filings, int cik) throws ParsingException {
        Map<String, List<?>> columns = new LinkedHashMap<>();
        columns.put("accessionNumber", filings.getAccessionNumber());
        columns.put("filingDate", filings.getFilingDate());
        columns.put("form", filings.getForm());
        columns.put("primaryDocument", filings.getPrimaryDocument());
        Set<Integer> sizes = new TreeSet<>();
        columns.values().forEach(column -> sizes.add(column == null ? -1 : column.size()));
        if (sizes.size() != 1 || sizes.contains(-1)) {
            Map<String, Integer> lengths = new LinkedHashMap<>();
            columns.forEach((key, column) -> lengths.put(key, column == null ? null : column.size()));
            throw new ParsingException("Filing columns missing or of different lengths: " + lengths);
        }
        return CikSubmissionParser.parse(filings, cik);
    }

    private void latestFeed(String source, String url, String rawName) throws IOException {
        byte[] raw = fetch(source, url);
        if (raw == null) {
            return;
        }
        List<FileRef> refs = parse(source, rawName, raw, () -> LatestFeedParser.parse(xml.readValue(raw, LatestFeed.class)));
        if (refs != null) {
            report.checked(source, refs.size() + " filings");
        }
    }

    private void dailyList(String source, String url, String rawName) throws IOException {
        byte[] raw = fetch(source, url);
        if (raw == null) {
            return;
        }
        List<FileRef> refs = parse(source, rawName, raw, () -> DailyFeedParser.parse(text(raw)));
        if (refs != null) {
            report.checked(source, refs.size() + " filings (none on weekends and holidays)");
        }
    }

    // The current quarter's index, plus the previous one when a form type is short of its sample.
    private List<FileRef> quarterlyIndexes() throws IOException {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<FileRef> refs = new ArrayList<>(quarterlyIndex(today));
        boolean anyShort = OWNERSHIP_SAMPLE.entrySet().stream()
                .anyMatch(e -> refs.stream().filter(ref -> ref.type() == e.getKey()).count() < e.getValue());
        if (anyShort) {
            refs.addAll(quarterlyIndex(today.minusMonths(3)));
        }
        return refs;
    }

    private List<FileRef> quarterlyIndex(LocalDate day) throws IOException {
        int year = day.getYear();
        int quarter = (day.getMonthValue() - 1) / 3 + 1;
        String source = "Quarterly index " + year + " Q" + quarter;
        String rawName = "full-index/master-" + year + "-QTR" + quarter + ".idx";
        byte[] raw = fetch(source, String.format(URL_INDEX, year, quarter));
        if (raw == null) {
            return List.of();
        }
        if (!new String(raw, StandardCharsets.ISO_8859_1).contains(INDEX_COLUMNS)) {
            report.finding(source, "format changed", "Column line `" + INDEX_COLUMNS + "` not found", rawName, raw);
        }
        List<FileRef> refs = parse(source, rawName, raw, () -> IndexFeedParser.parse(new ByteArrayInputStream(raw)));
        if (refs == null) {
            return List.of();
        }
        report.checked(source, refs.size() + " filings");
        return refs;
    }

    private void ownershipFilings(List<FileRef> index) throws IOException {
        OwnershipDocumentParser parser = new OwnershipDocumentParser(xml);
        Map<String, Integer> schemaVersions = new TreeMap<>();
        Map<FileType, Integer> sampled = new LinkedHashMap<>();

        for (Map.Entry<FileType, Integer> entry : OWNERSHIP_SAMPLE.entrySet()) {
            FileType type = entry.getKey();
            List<FileRef> recent = index.stream()
                    .filter(ref -> ref.type() == type)
                    .sorted(Comparator.comparing(FileRef::filedAt).thenComparing(FileRef::accNo).reversed())
                    .limit(entry.getValue())
                    .toList();
            sampled.put(type, recent.size());

            for (FileRef ref : recent) {
                String source = "Ownership " + type.getValue() + " " + ref.accNo();
                String rawName = "ownership/" + ref.accNo() + ".txt";
                byte[] raw = fetch(source, ref.buildUrl());
                if (raw == null) {
                    continue;
                }
                OwnershipDocument doc = parse(source, rawName, raw, () -> parser.parse(text(raw)));
                if (doc == null) {
                    continue;
                }
                String schemaVersion = doc.ownershipForm().getSchemaVersion();
                schemaVersions.merge(String.valueOf(schemaVersion), 1, Integer::sum);
                if (!known.schemaVersions().contains(schemaVersion)) {
                    report.finding(source, "unseen schemaVersion", schemaVersion + " is not in `specs/versions.json`", rawName, raw);
                }
                if (doc.type() != ref.type()) {
                    report.finding(source, "form type mismatch", "header says " + doc.type().getValue()
                            + ", index says " + ref.type().getValue(), rawName, raw);
                }
            }
        }
        StringBuilder summary = new StringBuilder();
        for (Iterator<Map.Entry<FileType, Integer>> it = sampled.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<FileType, Integer> entry = it.next();
            summary.append(entry.getValue()).append(" × ").append(entry.getKey().getValue()).append(it.hasNext() ? ", " : "");
        }
        report.checked("Ownership filings", summary + "; schemaVersions " + schemaVersions);
    }

    /** Fetches a response; a failed request is a finding too (the source may have moved). */
    private byte[] fetch(String source, String url) {
        try (InputStream stream = client.loadStream(url, "text/html")) {
            return stream.readAllBytes();
        } catch (HttpRequestException | IOException e) {
            report.finding(source, "request failed", url + ": " + e.getMessage());
            return null;
        }
    }

    /** Runs a parse, recording a failure or unknown fields as findings with the raw response. */
    private <T> T parse(String source, String rawName, byte[] raw, Parse<T> parse) throws IOException {
        unknown.drain();
        T result = null;
        try {
            result = parse.run();
        } catch (Exception e) {
            report.finding(source, "parse error", String.valueOf(e.getMessage() == null ? e : e.getMessage()), rawName, raw);
        }
        Set<String> fields = unknown.drain();
        if (!fields.isEmpty()) {
            report.finding(source, "unknown fields", String.join(", ", fields), rawName, raw);
        }
        return result;
    }

    private static String text(byte[] raw) {
        return new String(raw, StandardCharsets.UTF_8);
    }
}
