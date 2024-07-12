package com.alexkouzel.filing.f345;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.refs.FilingRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.filing.FilingUrlParser;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class OwnershipDocParser {

    private final XmlMapper xmlMapper;

    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

    public OwnershipDoc parse(String data, FilingRef ref) throws ParsingException {
        return new OwnershipDoc(
                ref.accNum(),
                ref.getTxtUrl(),
                getXmlUrl(data, ref),
                ref.filedAt(),
                getXmlForm(data)
        );
    }

    private String getXmlUrl(String data, FilingRef ref) {
        String filename = StringUtils.substring(data, "<FILENAME>", "\n");
        return FilingUrlParser.getXmlUrl(ref.issuerCik(), ref.accNum(), filename);
    }

    private OwnershipForm getXmlForm(String data) throws ParsingException {
        try {
            String xml = getXmlData(data);
            return xmlMapper.readValue(xml, OwnershipForm.class);

        } catch (JsonProcessingException e) {
            throw new ParsingException("Failed mapping OwnershipForm: " + e.getMessage());
        }
    }

    private String getXmlData(String data) throws ParsingException {
        String xml = StringUtils.substring(data, "<XML>", "</XML>");
        if (xml == null) throw new ParsingException("<XML> is missing");

        xml = xml.trim();
        if (xml.startsWith("<xml>")) {
            xml = StringUtils.removeFirstLine(xml);
        }
        xml = NEW_LINE_PATTERN.matcher(xml).replaceAll("");
        return xml;
    }

}
