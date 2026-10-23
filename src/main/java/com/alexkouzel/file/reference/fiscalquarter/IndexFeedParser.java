package com.alexkouzel.file.reference.fiscalquarter;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses a quarterly master.idx: a free-form header, a line of dashes, then rows of
 * {@code CIK|Company Name|Form Type|Date Filed|Filename}. Each filing is returned once.
 */
@UtilityClass
public class IndexFeedParser {

    public List<FileRef> parse(InputStream stream) throws ParsingException {
        List<FileRef> fileRefs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Company names aren't guaranteed to be UTF-8; ISO-8859-1 never fails to decode.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.ISO_8859_1))) {
            boolean inRows = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!inRows) {
                    inRows = line.startsWith("---");
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                FileRef ref = parseLine(line);
                if (seen.add(ref.accNo())) {
                    fileRefs.add(ref);
                }
            }
            if (!inRows) {
                throw new ParsingException("No header separator (line of dashes) found");
            }
        } catch (IOException e) {
            throw new ParsingException("Failed to read index: " + e.getMessage());
        }
        return fileRefs;
    }

    private FileRef parseLine(String line) throws ParsingException {
        String[] parts = line.split("\\|");

        if (parts.length != 5) {
            throw new ParsingException("Invalid number of data parts: " + line);
        }

        // Parse accession number
        String filename = parts[4].trim();
        int accNoIdx = filename.lastIndexOf("/") + 1;
        String accNo = filename.substring(accNoIdx, filename.length() - ".txt".length());

        // Parse issuer CIK
        int issuerCik = Integer.parseInt(parts[0].trim());

        // Parse file type
        FileType type = FileType.ofValue(parts[2].trim());

        // Parse file date
        LocalDate filedAt = DateUtils.parse(parts[3].trim(), "yyyy-MM-dd");

        return new FileRef(accNo, issuerCik, type, filedAt);
    }
}
