package com.alexkouzel.filing;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.metadata.FilingMetadata;
import com.alexkouzel.filing.metadata.FilingMetadataLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilingMetadataLoaderTest {

    private static final FilingMetadataLoader loader = new FilingMetadataLoader(new TestEdgarClient());

    @Test
    public void loadByCik() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadByCik(FilingType.F4, "0000946581");
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    @Test
    public void loadByQuarter() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadByQuarter(FilingType.F4, 2022, 3);
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    @Test
    public void loadDaysAgo() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadDaysAgo(FilingType.F4, 1);
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    @Test
    public void loadLatest() throws HttpRequestException, ParsingException {
        List<FilingMetadata> metadata = loader.loadLatest(FilingType.F4, 0, 200);
        assertFalse(metadata.isEmpty());
        verifyMetadata(metadata.get(0));
    }

    private void verifyMetadata(FilingMetadata metadata) {
        assertNotNull(metadata);
        assertNotNull(metadata.accNum());
        assertNotNull(metadata.issuerCik());
        assertNotNull(metadata.type());
        assertNotNull(metadata.filedAt());

        assertEquals(20, metadata.accNum().length());
        assertTrue(metadata.issuerCik().length() <= 10);
    }

}
