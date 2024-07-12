package com.alexkouzel.filing.refs;

import com.alexkouzel.client.EdgarClient;
import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.FilingType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilingRefLoaderTest {

    private static final EdgarClient client = new EdgarClient("TestCompany test@gmail.com");
    private static final FilingRefLoader loader = new FilingRefLoader(client);

    @Test
    public void loadByCik() throws RequestFailedException, ParsingException {
        List<FilingRef> refs = loader.loadByCik(FilingType.F4, "0000946581");
        assertFalse(refs.isEmpty());
        verifyRef(refs.get(0));
    }

    @Test
    public void loadByQuarter() throws RequestFailedException, ParsingException {
        List<FilingRef> refs = loader.loadByQuarter(FilingType.F4, 2022, 3);
        assertFalse(refs.isEmpty());
        verifyRef(refs.get(0));
    }

    @Test
    public void loadDaysAgo() throws RequestFailedException, ParsingException {
        List<FilingRef> refs = loader.loadDaysAgo(FilingType.F4, 1);
        assertFalse(refs.isEmpty());
        verifyRef(refs.get(0));
    }

    @Test
    public void loadLatest() throws RequestFailedException, ParsingException {
        List<FilingRef> refs = loader.loadLatest(FilingType.F4, 0, 200);
        assertFalse(refs.isEmpty());
        verifyRef(refs.get(0));
    }

    private void verifyRef(FilingRef ref) {
        assertNotNull(ref);
        assertNotNull(ref.accNum());
        assertNotNull(ref.issuerCik());
        assertNotNull(ref.type());
        assertNotNull(ref.filedAt());

        assertEquals(20, ref.accNum().length());
        assertTrue(ref.issuerCik().length() <= 10);
    }

}
