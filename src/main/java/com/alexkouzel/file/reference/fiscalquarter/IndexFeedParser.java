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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class IndexFeedParser {

    public List<FileRef> parse(InputStream stream) throws ParsingException {
        List<FileRef> fileRefs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            int idx = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (idx >= 11) {
                    fileRefs.add(parseLine(line));
                }
                idx++;
            }
        } catch (IOException e) {
            throw new ParsingException("Failed to read a line");
        }
        return fileRefs;
    }

    private FileRef parseLine(String line) throws ParsingException {
        String[] parts = line.split("\\|");

        if (parts.length != 5) {
            throw new ParsingException("Invalid number of data parts");
        }

        // Parse accession number
        int accNoIdx = parts[4].lastIndexOf("/") + 1;
        String accNo = parts[4].substring(accNoIdx, parts[4].length() - 4);

        // Parse issuer CIK
        int issuerCik = Integer.parseInt(parts[0]);

        // Parse file type
        FileType type = FileType.ofValue(parts[2]);

        // Parse file date
        LocalDate filedAt = DateUtils.parse(parts[3], "yyyy-MM-dd");

        return new FileRef(accNo, issuerCik, type, filedAt);
    }
}
