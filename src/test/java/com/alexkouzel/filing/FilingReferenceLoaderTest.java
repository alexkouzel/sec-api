package com.alexkouzel.filing;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.reference.FilingReference;
import com.alexkouzel.filing.reference.FilingReferenceLoader;
import com.alexkouzel.filing.reference.latest.LatestFeedCount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilingReferenceLoaderTest {
    private static final FilingReferenceLoader loader = new FilingReferenceLoader(new TestEdgarClient());

    @Test
    public void loadByCik() throws HttpRequestException {
        List<FilingReference> ref = loader.loadByCik(946581);
        assertFalse(ref.isEmpty());
        verifyRef(ref.getFirst());
    }

    @Test
    public void loadByFiscalQuarter() throws HttpRequestException, ParsingException {
        List<FilingReference> ref = loader.loadByFiscalQuarter(2022, 3);
        assertFalse(ref.isEmpty());
        verifyRef(ref.getFirst());
    }

    @Test
    public void loadDaysAgo() throws HttpRequestException, ParsingException {
        List<FilingReference> ref = loader.loadDaysAgo(1);
        assertFalse(ref.isEmpty());
        verifyRef(ref.getFirst());
    }

    @Test
    public void loadLatest() throws HttpRequestException, ParsingException {
        List<FilingReference> ref = loader.loadLatest(0, LatestFeedCount.TEN);
        assertFalse(ref.isEmpty());
        verifyRef(ref.getFirst());
    }

    private void verifyRef(FilingReference ref) {
        assertNotNull(ref);
        assertNotNull(ref.accNo());
        assertNotEquals(0, ref.issuerCik());
        assertNotNull(ref.type());
        assertNotNull(ref.filedAt());
        assertEquals(20, ref.accNo().length());
    }
}
