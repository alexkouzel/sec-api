package com.alexkouzel.filing.refs.index;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.refs.FilingRef;
import com.alexkouzel.common.utils.DateUtils;
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

    public List<FilingRef> parse(InputStream stream) throws ParsingException {
        List<FilingRef> refs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            int idx = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (idx >= 11) refs.add(parseLine(line));
                idx++;
            }
        } catch (IOException e) {
            throw new ParsingException("Failed to read a line");
        }
        return refs;
    }

    private FilingRef parseLine(String line) throws ParsingException {
        String[] parts = line.split("\\|");

        if (parts.length != 5)
            throw new ParsingException("Invalid number of data parts");

        // Parse accession number
        int accNumIdx = parts[4].lastIndexOf("/") + 1;
        String accNum = parts[4].substring(accNumIdx, parts[4].length() - 4);

        // Parse issuer CIK
        String issuerCik = parts[0];

        // Parse filing type
        FilingType type = FilingType.ofValue(parts[2]);

        // Parse filing date
        LocalDate filedAt = DateUtils.parse(parts[3], "yyyy-MM-dd");

        return new FilingRef(accNum, issuerCik, type, filedAt);
    }

}
