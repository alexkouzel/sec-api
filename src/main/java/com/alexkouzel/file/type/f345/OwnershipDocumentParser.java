package com.alexkouzel.file.type.f345;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class OwnershipDocumentParser {
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

    private final XmlMapper xmlMapper;

    public OwnershipDocument parse(String data, FileRef fileRef) throws ParsingException {
        return new OwnershipDocument(
                fileRef.accNo(),
                fileRef.type(),
                fileRef.filedAt(),
                parseXmlFilename(data),
                parseOwnershipForm(data)
        );
    }

    public OwnershipDocument parse(String data) throws ParsingException {
        String header = StringUtils.until(data, "\n\n");
        String[] lines = header.split("\n");

        String accNo = getHeaderValue(lines[3]);
        String type = getHeaderValue(lines[4]);
        String filedAt = getHeaderValue(lines[7]);

        return new OwnershipDocument(
                accNo,
                FileType.ofValue(type),
                DateUtils.parse(filedAt, "yyyyMMdd"),
                parseXmlFilename(data),
                parseOwnershipForm(data)
        );
    }

    private String getHeaderValue(String line) {
        int valueIdx = line.lastIndexOf("\t") + 1;
        return line.substring(valueIdx);
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
        if (xml == null) {
            throw new ParsingException("<XML> is missing");
        }
        xml = xml.trim();
        if (xml.startsWith("<xml>")) {
            xml = StringUtils.removeFirstLine(xml);
        }
        xml = NEW_LINE_PATTERN.matcher(xml).replaceAll("");
        return xml;
    }
}
