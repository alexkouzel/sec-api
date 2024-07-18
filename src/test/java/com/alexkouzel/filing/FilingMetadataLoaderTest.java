package com.alexkouzel.filing;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.metadata.FilingMetadata;
import com.alexkouzel.filing.metadata.FilingMetadataLoader;
import com.alexkouzel.filing.metadata.latest.LatestFeedCount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilingMetadataLoaderTest {

    private static final FilingMetadataLoader loader = new FilingMetadataLoader(new TestEdgarClient());

    @Test
    public void loadByCik() throws HttpRequestException {
        List<FilingMetadata> metadata = loader.loadByCik("0000946581");
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    @Test
    public void loadByQuarter() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadByQuarter(2022, 3);
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    @Test
    public void loadDaysAgo() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadDaysAgo(1);
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    @Test
    public void loadLatest() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadLatest(0, LatestFeedCount.TEN);
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    private void verifyMetadata(FilingMetadata metadata) {
        assertNotNull(metadata);
        assertNotNull(metadata.accNo());
        assertNotNull(metadata.issuerCik());
        assertNotNull(metadata.type());
        assertNotNull(metadata.filedAt());

        assertEquals(20, metadata.accNo().length());
        assertTrue(metadata.issuerCik().length() <= 10);
    }

}
