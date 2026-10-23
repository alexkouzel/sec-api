package com.alexkouzel.live;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.company.SecCompany;
import com.alexkouzel.company.SecCompanyLoader;
import com.alexkouzel.file.FileLoader;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import com.alexkouzel.file.reference.FileRefLoader;
import com.alexkouzel.file.reference.latest.LatestFilesLimit;
import com.alexkouzel.file.type.f345.FileF345;
import com.alexkouzel.file.type.f345.OwnershipForm;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Smoke tests against live EDGAR: every source answers and parses. Exact values
 * are checked offline against fixtures. Run with ./gradlew liveTest.
 */
@Tag("live")
public class EdgarLiveTest {
    private static final TestEdgarClient client = new TestEdgarClient();
    private static final FileRefLoader refs = new FileRefLoader(client);

    private static void assertValid(FileRef ref) {
        assertEquals(20, ref.accNo().length(), ref.accNo());
        assertTrue(ref.issuerCik() > 0);
        assertNotNull(ref.type());
        assertNotNull(ref.filedAt());
    }

    @Test
    public void companies() throws Exception {
        List<SecCompany> companies = new SecCompanyLoader(client).load();

        assertTrue(companies.size() > 5_000, "only " + companies.size());
        assertTrue(companies.stream().anyMatch(c -> c.ticker().equals("AAPL")));
    }

    @Test
    public void companyFilings() throws Exception {
        List<FileRef> filings = refs.loadCompany(320193);

        assertFalse(filings.isEmpty());
        filings.forEach(EdgarLiveTest::assertValid);
    }

    @Test
    public void latestFilings() throws Exception {
        List<FileRef> filings = refs.loadLatest(0, LatestFilesLimit.FORTY);

        assertFalse(filings.isEmpty());
        filings.forEach(EdgarLiveTest::assertValid);
    }

    @Test
    public void dailyFilings() throws Exception {
        List<FileRef> filings = refs.loadDaysAgo(1);

        assertFalse(filings.isEmpty());
        filings.forEach(EdgarLiveTest::assertValid);
    }

    @Test
    public void previousQuarterIndex() throws Exception {
        LocalDate quarterAgo = LocalDate.now().minusMonths(3);
        List<FileRef> filings = refs.loadFiscalQuarter(quarterAgo.getYear(), (quarterAgo.getMonthValue() - 1) / 3 + 1);

        assertTrue(filings.size() > 10_000, "only " + filings.size());
        assertValid(filings.getFirst());
    }

    @Test
    public void recentFormFour() throws Exception {
        // The daily page has hundreds of Form 4s; the latest feed often has none (prefix filter).
        Optional<FileRef> ref = refs.loadDaysAgo(1, FileType.F4).stream()
                .filter(r -> r.type() == FileType.F4)
                .findFirst();
        assumeTrue(ref.isPresent(), "no Form 4 filed one day ago");

        FileF345 file = new FileLoader(client).loadF345ByRef(ref.get());

        OwnershipForm form = file.getOwnershipDocument().ownershipForm();
        assertEquals("4", form.getDocumentType());
        assertNotNull(form.getIssuer().getIssuerName());
        assertFalse(form.getReportingOwner().isEmpty());
    }
}
