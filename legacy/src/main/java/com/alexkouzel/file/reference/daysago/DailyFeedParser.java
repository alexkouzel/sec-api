package com.alexkouzel.file.reference.daysago;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.common.utils.StringUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the daily filings page (cgi-bin/current): one row per filing and related CIK,
 * between two {@code <hr>} tags, e.g.
 * <pre>10-21-2026   &lt;a href="/Archives/edgar/data/1553209/0001099910-26-000286-index.html"&gt;4&lt;/a&gt; ...</pre>
 * Each filing is returned once, in page order, with the CIK of its first row. The page
 * doesn't say which role that CIK has.
 */
@UtilityClass
public class DailyFeedParser {
    private final Pattern ROW_PATTERN = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+<a href=\"/Archives/edgar/data/(\\d+)/(\\d{10}-\\d{2}-\\d{6})-index\\.html?\">([^<]+)</a>");

    public List<FileRef> parse(String data) throws ParsingException {
        String hr = StringUtils.substring(data, "<hr>", "<hr>");
        if (hr == null) {
            throw new ParsingException("<hr> field is missing");
        }
        List<FileRef> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String row : hr.split("\n")) {
            if (row.isBlank()) {
                continue;
            }
            FileRef ref = parseRow(row);
            if (seen.add(ref.accNo())) {
                result.add(ref);
            }
        }
        return result;
    }

    private FileRef parseRow(String row) throws ParsingException {
        Matcher matcher = ROW_PATTERN.matcher(row.trim());
        if (!matcher.find()) {
            throw new ParsingException("Unrecognized daily filings row: " + row);
        }
        LocalDate filedAt = DateUtils.parse(matcher.group(1), "MM-dd-yyyy");
        int cik = Integer.parseInt(matcher.group(2));
        String accNo = matcher.group(3);
        FileType type = FileType.ofValue(matcher.group(4).trim());
        return new FileRef(accNo, cik, type, filedAt);
    }
}
