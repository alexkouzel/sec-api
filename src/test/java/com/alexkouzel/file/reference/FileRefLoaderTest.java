package com.alexkouzel.file.reference;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.daysago.DailyFeedParser;
import com.alexkouzel.file.reference.fiscalquarter.IndexFeedParser;
import com.alexkouzel.file.reference.latest.LatestFilesLimit;
import com.alexkouzel.testing.FixtureClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileRefLoaderTest {
    private static final String COMPANY_URL = "https://data.sec.gov/submissions/CIK0000946581.json";
    private static final String LATEST_URL = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent";
    private static final String LATEST_10_URL = LATEST_URL + "&start=0&count=10&output=atom";
    private static final String LATEST_40_F4_URL = LATEST_URL + "&start=0&count=40&output=atom&type=4";
    private static final String LATEST_20_URL = LATEST_URL + "&start=0&count=20&output=atom";
    private static final String DAYS_AGO_1_URL = "https://www.sec.gov/cgi-bin/current?q1=1";
    private static final String DAYS_AGO_1_F4_URL = DAYS_AGO_1_URL + "&q3=4";
    private static final String QUARTER_URL = "https://www.sec.gov/Archives/edgar/full-index/%d/QTR%d/master.idx";

    private final FixtureClient client = new FixtureClient()
            .route(COMPANY_URL, "submissions/CIK0000946581.json")
            .route(LATEST_10_URL, "latest/getcurrent-count-10.atom")
            .route(LATEST_40_F4_URL, "latest/getcurrent-count-40-type-4.atom")
            .route(LATEST_20_URL, "latest/getcurrent-count-20-type-SCHEDULE13.atom")
            .route(DAYS_AGO_1_URL, "current/current-q1-1.html")
            .route(DAYS_AGO_1_F4_URL, "current/current-q1-1-q3-4.html")
            .route(QUARTER_URL.formatted(2026, 4), "full-index/master-2026-QTR4.idx")
            .route(QUARTER_URL.formatted(2022, 3), "full-index/master-2022-QTR3.idx");

    private final FileRefLoader loader = new FileRefLoader(client);

    private static FileRef ref(String accNo, int cik, FileType type, String filedAt) {
        return new FileRef(accNo, cik, type, LocalDate.parse(filedAt));
    }

    private static long countUnique(List<FileRef> refs) {
        return refs.stream().map(FileRef::accNo).distinct().count();
    }

    @Nested
    class Company {

        @Test
        public void loadsRecentFilings() throws Exception {
            List<FileRef> refs = loader.loadCompany(946581);

            assertEquals(40, refs.size());
            assertEquals(ref("0000946581-26-000104", 946581, FileType.F4, "2026-10-16"), refs.getFirst());
            assertEquals(ref("0001950047-26-006756", 946581, FileType.OTHER, "2026-07-01"), refs.getLast());
            assertEquals(21, refs.stream().filter(r -> r.type() == FileType.F4).count());
            assertTrue(refs.stream().allMatch(r -> r.issuerCik() == 946581));
        }
    }

    @Nested
    class Latest {

        @Test
        public void parsesEntries() throws Exception {
            List<FileRef> refs = loader.loadLatest(0, LatestFilesLimit.TEN);

            assertEquals(ref("0001213900-26-111931", 2028201, FileType.OTHER, "2026-10-23"), refs.getFirst());
            assertEquals(ref("0001193125-26-429780", 2086532, FileType.OTHER, "2026-10-23"), refs.getLast());
        }

        @Test
        public void includesFormFourFilings() throws Exception {
            List<FileRef> refs = loader.loadLatest(LatestFilesLimit.FORTY, FileType.F4);

            assertTrue(refs.contains(ref("0001493152-26-048187", 1515139, FileType.F4, "2026-10-23")));
        }

        @Test
        public void typeFilterReturnsOnlyThatType() throws Exception {
            List<FileRef> refs = loader.loadLatest(LatestFilesLimit.FORTY, FileType.F4);

            assertEquals(List.of(FileType.F4), refs.stream().map(FileRef::type).distinct().toList());
        }

        @Test
        public void returnsEachFilingOnce() throws Exception {
            List<FileRef> refs = loader.loadLatest(0, LatestFilesLimit.TEN);

            assertEquals(5, refs.size());
            assertEquals(refs.size(), countUnique(refs));
        }

        @Test
        public void issuerCikComesFromIssuerEntry() throws Exception {
            List<FileRef> refs = loader.loadLatest(LatestFilesLimit.FORTY, FileType.F4);

            List<FileRef> matching = refs.stream()
                    .filter(r -> r.accNo().equals("0001493152-26-048187"))
                    .toList();
            assertEquals(List.of(ref("0001493152-26-048187", 1515139, FileType.F4, "2026-10-23")), matching);
        }

        @Test
        public void subjectCompanyCikWinsOverFiler() throws Exception {
            // Fixture captured with type=SCHEDULE 13; routed here without the filter.
            List<FileRef> refs = loader.loadLatest(0, LatestFilesLimit.TWENTY);

            assertEquals(10, refs.size());
            // "Filed by" entry comes first for this one, "Subject" for the first filing.
            assertTrue(refs.contains(ref("0001493152-26-048185", 1986694, FileType.OTHER, "2026-10-23")));
            assertEquals(ref("0000038777-26-000271", 1762562, FileType.OTHER, "2026-10-23"), refs.getFirst());
        }

        @Test
        public void typeFilterIsUrlEncoded() throws Exception {
            String url = LATEST_URL + "&start=0&count=10&output=atom&type=4%2FA";
            client.route(url, "latest/getcurrent-count-10.atom");

            assertEquals(List.of(), loader.loadLatest(0, LatestFilesLimit.TEN, FileType.F4_A));
            assertEquals(url, client.requestedUrls().getLast());
        }
    }

    @Nested
    class DaysAgo {

        @Test
        public void parsesRows() throws Exception {
            List<FileRef> refs = loader.loadDaysAgo(1);

            assertEquals(ref("0000944388-26-000014", 944388, FileType.OTHER, "2026-10-21"), refs.getFirst());
            assertEquals(ref("0002071844-26-001439", 1112996, FileType.OTHER, "2026-10-21"), refs.getLast());
        }

        @Test
        public void includesFormFourFilings() throws Exception {
            List<FileRef> refs = loader.loadDaysAgo(1, FileType.F4);

            assertEquals(ref("0001099910-26-000286", 1553209, FileType.F4, "2026-10-21"), refs.getFirst());
            assertEquals(33, refs.size());
        }

        @Test
        public void typeFilterReturnsOnlyThatType() throws Exception {
            List<FileRef> refs = loader.loadDaysAgo(1, FileType.F4);

            assertEquals(List.of(FileType.F4), refs.stream().map(FileRef::type).distinct().toList());
        }

        @Test
        public void returnsEachFilingOnce() throws Exception {
            List<FileRef> refs = loader.loadDaysAgo(1);

            assertEquals(52, refs.size());
            assertEquals(refs.size(), countUnique(refs));
        }

        @Test
        public void rejectsUnrecognizedRow() {
            String page = "<pre><hr>10-21-2026   something else entirely\n<hr></pre>";

            assertThrows(ParsingException.class, () -> DailyFeedParser.parse(page));
        }
    }

    @Nested
    class FiscalQuarter {

        @Test
        public void parsesCurrentIndex() throws Exception {
            List<FileRef> refs = loader.loadFiscalQuarter(2026, 4);

            assertEquals(200, refs.size());
            assertEquals(ref("0000919574-26-006682", 1000177, FileType.F4, "2026-10-06"), refs.getFirst());
            assertEquals(ref("0000950103-26-015862", 1000275, FileType.OTHER, "2026-10-20"), refs.getLast());
        }

        @Test
        public void parsesOlderIndex() throws Exception {
            List<FileRef> refs = loader.loadFiscalQuarter(2022, 3);

            assertEquals(200, refs.size());
            assertEquals(ref("0000950170-22-016293", 1000045, FileType.Q10, "2022-08-10"), refs.getFirst());
            assertEquals(14, refs.stream().filter(r -> r.type() == FileType.F4).count());
        }

        @Test
        public void headerLengthDoesNotMatter() throws Exception {
            String index = "Description: Master Index\nAn extra header line\n\n"
                    + "CIK|Company Name|Form Type|Date Filed|Filename\n"
                    + "--------------------------------------------------------------------------------\n"
                    + "1000177|NORDIC AMERICAN TANKERS Ltd|4|2026-10-06|edgar/data/1000177/0000919574-26-006682.txt\n";

            List<FileRef> refs = IndexFeedParser.parse(new ByteArrayInputStream(index.getBytes(StandardCharsets.ISO_8859_1)));

            assertEquals(List.of(ref("0000919574-26-006682", 1000177, FileType.F4, "2026-10-06")), refs);
        }

        @Test
        public void rejectsIndexWithoutSeparator() {
            String index = "CIK|Company Name|Form Type|Date Filed|Filename\n";

            assertThrows(ParsingException.class,
                    () -> IndexFeedParser.parse(new ByteArrayInputStream(index.getBytes(StandardCharsets.ISO_8859_1))));
        }
    }
}
