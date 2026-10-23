package com.alexkouzel.drift;

import com.alexkouzel.file.reference.company.CikSubmission;
import com.alexkouzel.file.type.f345.OwnershipDocumentParser;
import com.alexkouzel.testing.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UnknownFieldCollectorTest {
    private final UnknownFieldCollector collector = new UnknownFieldCollector();

    @Test
    public void knownXmlRecordsNothing() throws Exception {
        new OwnershipDocumentParser(collector.xmlMapper()).parse(Fixtures.text("ownership/0001315098-26-000186.txt"));

        assertEquals(Set.of(), collector.drain());
    }

    @Test
    public void recordsUnknownXmlElementsAndKeepsParsing() throws Exception {
        String data = Fixtures.text("ownership/0001315098-26-000186.txt")
                .replace("<rptOwnerCik>", "<rptOwnerNewThing><a>1</a></rptOwnerNewThing><rptOwnerCik>")
                .replace("<issuerCik>", "<issuerNewThing>x</issuerNewThing><issuerCik>");

        var doc = new OwnershipDocumentParser(collector.xmlMapper()).parse(data);

        assertEquals(Set.of("Issuer.issuerNewThing", "ReportingOwner.ID.rptOwnerNewThing"), collector.drain());
        assertNotNull(doc.ownershipForm().getReportingOwner().getFirst().getReportingOwnerId().getRptOwnerCik());
        assertEquals(Set.of(), collector.drain());
    }

    @Test
    public void recordsUnknownJsonFields() throws Exception {
        String json = Fixtures.text("submissions/CIK0000946581.json")
                .replace("\"accessionNumber\":", "\"newColumn\":[1,2],\"accessionNumber\":")
                .replaceFirst("\"cik\":", "\"newTopLevel\":{\"a\":1},\"cik\":");

        collector.jsonMapper().readValue(json, CikSubmission.class);

        assertEquals(Set.of("CikSubmission.newTopLevel", "CikSubmissionFilings.Recent.newColumn"), collector.drain());
    }
}
