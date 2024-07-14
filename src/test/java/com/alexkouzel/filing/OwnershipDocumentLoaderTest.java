package com.alexkouzel.filing;

import com.alexkouzel.client.TestEdgarClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.f345.OwnershipDocument;
import com.alexkouzel.filing.f345.OwnershipDocumentLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OwnershipDocumentLoaderTest {

    private static final OwnershipDocumentLoader loader = new OwnershipDocumentLoader(new TestEdgarClient());

    @Test
    public void loadByUrl() throws HttpRequestException, ParsingException {
        String url = "https://www.sec.gov/Archives/edgar/data/1000753/000112760223028345/0001127602-23-028345.txt";
        OwnershipDocument doc = loader.loadByUrl(url);
        verifyDoc(doc);
    }

    private void verifyDoc(OwnershipDocument doc) {
        assertNotNull(doc);
    }

}
