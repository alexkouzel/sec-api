package com.alexkouzel.filing.type.f345;

import com.alexkouzel.client.HttpDataClient;
import com.alexkouzel.client.exceptions.HttpRequestException;
import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.reference.FilingReference;
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

    public OwnershipDocument loadByTxtUrl(String txtUrl) throws HttpRequestException, ParsingException {
        String data = client.loadText(txtUrl);
        return parser.parse(data);
    }

    public OwnershipDocument loadByRef(FilingReference ref) throws HttpRequestException, ParsingException {
        String data = client.loadText(ref.getTxtUrl());
        return parser.parse(data, ref);
    }
}
