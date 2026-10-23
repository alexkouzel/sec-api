package com.alexkouzel.file.type.f345;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.file.reference.FileRef;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class OwnershipDocumentLoader {
    private final OwnershipDocumentParser parser;
    private final HttpDataClient client;

    public OwnershipDocumentLoader(HttpDataClient client, XmlMapper xmlMapper) {
        this.parser = new OwnershipDocumentParser(xmlMapper);
        this.client = client;
    }

    public OwnershipDocumentLoader(HttpDataClient client) {
        this(client, buildXmlMapper());
    }

    // EDGAR adds schema elements over time; unknown ones must not break parsing.
    private static XmlMapper buildXmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public OwnershipDocument loadByUrl(String url) throws HttpRequestException, ParsingException {
        String data = client.loadText(url);
        return parser.parse(data);
    }

    public OwnershipDocument loadByFileRef(FileRef fileRef) throws HttpRequestException, ParsingException {
        String data = client.loadText(fileRef.buildUrl());
        return parser.parse(data, fileRef);
    }
}
