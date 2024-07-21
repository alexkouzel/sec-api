package com.alexkouzel.filing;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.filing.type.f345.OwnershipDocument;
import com.alexkouzel.filing.type.f345.OwnershipDocumentLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OwnershipDocumentLoaderTest {

    private static final OwnershipDocumentLoader loader = new OwnershipDocumentLoader(new TestEdgarClient());

    @Test
    public void loadByTxtUrl() throws HttpRequestException, ParsingException {
        String url = "https://www.sec.gov/Archives/edgar/data/1000753/000112760223028345/0001127602-23-028345.txt";
        verifyDoc1(loader.loadByTxtUrl(url));
    }

    private void verifyDoc1(OwnershipDocument actual) {
        OwnershipDocument expected = new OwnershipDocument(
                "0001127602-23-028345",
                FilingType.F4,
                DateUtils.parse("2023-11-28", "yyyy-MM-dd"),
                "form4.xml",
                null
        );
        verifyEquals(expected, actual);
    }

    private void verifyEquals(OwnershipDocument expected, OwnershipDocument actual) {
        assertNotNull(actual);
        assertEquals(expected.accNo(), actual.accNo());
        assertEquals(expected.type(), actual.type());
        assertEquals(expected.filedAt(), actual.filedAt());
        assertEquals(expected.xmlFilename(), actual.xmlFilename());
    }

}
