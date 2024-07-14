package com.alexkouzel.filing.f345;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.filing.metadata.FilingMetadata;
import com.alexkouzel.client.HttpDataClient;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class OwnershipDocumentLoader {

    private final OwnershipDocumentParser parser;

    private final HttpDataClient client;

    public OwnershipDocumentLoader(HttpDataClient client, XmlMapper xmlMapper) {
        this.parser = new OwnershipDocumentParser(xmlMapper);
        this.client = client;
    }

    public OwnershipDocumentLoader(HttpDataClient client) {
        this(client, new XmlMapper());
    }

    public OwnershipDocument loadByUrl(String url) throws HttpRequestException, ParsingException {
        String data = client.loadText(url);
        return parser.parse(data);
    }

    public OwnershipDocument loadByMetadata(FilingMetadata metadata) throws HttpRequestException, ParsingException {
        return loadByUrl(metadata.getTxtUrl());
    }

}
