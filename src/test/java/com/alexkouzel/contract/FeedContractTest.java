package com.alexkouzel.contract;

import com.alexkouzel.file.reference.company.CikSubmission;
import com.alexkouzel.file.reference.latest.LatestFeed;
import com.alexkouzel.testing.Fixtures;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Strict parsing of the JSON and Atom fixtures, so new EDGAR fields are noticed.
 */
public class FeedContractTest {

    @ParameterizedTest
    @ValueSource(strings = {"submissions/CIK0000946581.json"})
    public void submissionsParseStrictly(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        assertNotNull(mapper.readValue(Fixtures.text(path), CikSubmission.class).getFilings().getRecent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest/getcurrent-count-10.atom", "latest/getcurrent-count-40-type-4.atom"})
    public void latestFeedParsesStrictly(String path) throws Exception {
        XmlMapper mapper = new XmlMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        assertNotNull(mapper.readValue(Fixtures.text(path), LatestFeed.class).getEntry());
    }
}
