package com.alexkouzel.file.type.f345;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
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

    /**
     * Reads accession number, form type and filed date from the SGML header by label,
     * so added or reordered header lines don't matter.
     */
    public OwnershipDocument parse(String data) throws ParsingException {
        String header = StringUtils.substring(data, "<SEC-HEADER>", "</SEC-HEADER>");
        if (header == null) {
            throw new ParsingException("<SEC-HEADER> is missing");
        }
        String accNo = getHeaderValue(header, "ACCESSION NUMBER");
        String type = getHeaderValue(header, "CONFORMED SUBMISSION TYPE");
        String filedAt = getHeaderValue(header, "FILED AS OF DATE");

        return new OwnershipDocument(
                accNo,
                FileType.ofValue(type),
                parseFiledAt(filedAt),
                parseXmlFilename(data),
                parseOwnershipForm(data)
        );
    }

    private String getHeaderValue(String header, String label) throws ParsingException {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(label) + ":[ \t]*(.+?)[ \t]*\r?$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(header);
        if (!matcher.find()) {
            throw new ParsingException("Header value is missing: " + label);
        }
        return matcher.group(1);
    }

    private LocalDate parseFiledAt(String value) throws ParsingException {
        try {
            return DateUtils.parse(value, "yyyyMMdd");
        } catch (DateTimeParseException e) {
            throw new ParsingException("Invalid FILED AS OF DATE: " + value);
        }
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
