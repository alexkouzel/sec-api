package com.alexkouzel.filing.f345;

import com.alexkouzel.common.exceptions.ParsingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.alexkouzel.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class OwnershipDocumentParser {

    private final XmlMapper xmlMapper;

    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

    public OwnershipDocument parse(String data) throws ParsingException {
        return new OwnershipDocument(
                parseXmlFilename(data),
                parseOwnershipForm(data));
    }

    private String parseXmlFilename(String data) {
        return StringUtils.substring(data, "<FILENAME>", "\n");
    }

    private OwnershipForm parseOwnershipForm(String data) throws ParsingException {
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
