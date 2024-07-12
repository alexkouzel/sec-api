package com.alexkouzel.filing.f345;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.client.exceptions.RequestFailedException;
import com.alexkouzel.filing.refs.FilingRef;
import com.alexkouzel.client.DataClient;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class OwnershipDocLoader {

    private final OwnershipDocParser parser;

    private final DataClient client;

    public OwnershipDocLoader(DataClient client, XmlMapper xmlMapper) {
        this.parser = new OwnershipDocParser(xmlMapper);
        this.client = client;
    }

    public OwnershipDocLoader(DataClient client) {
        this(client, new XmlMapper());
    }

    public OwnershipDoc loadByRef(FilingRef ref) throws RequestFailedException, ParsingException {
        String txtData = client.loadText(ref.getTxtUrl());
        return parser.parse(txtData, ref);
    }

}
