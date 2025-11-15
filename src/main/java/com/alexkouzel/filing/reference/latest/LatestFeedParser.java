package com.alexkouzel.filing.reference.latest;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.filing.FilingType;
import com.alexkouzel.filing.reference.FilingReference;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class LatestFeedParser {
    private final Pattern ISSUER_CIK_PATTERN = Pattern.compile("data/(\\d+)");

    public List<FilingReference> parse(LatestFeed feed) throws ParsingException {
        List<FilingReference> refs = new ArrayList<>();
        for (LatestFeedEntry entry : feed.getEntry()) {
            refs.add(parseEntry(entry));
        }
        return refs;
    }

    private FilingReference parseEntry(LatestFeedEntry entry) throws ParsingException {
        String[] summaryParts = entry.getSummary().split(" ");

        // Parse accession number
        String accNo = summaryParts[4];

        // Parse issuer CIK
        String href = entry.getLink().getHref();
        Matcher matcher = ISSUER_CIK_PATTERN.matcher(href);
        String issuerCikValue = matcher.find() ? matcher.group(1) : null;

        if (issuerCikValue == null)
            throw new ParsingException("Couldn't match issuer CIK at href");

        int issuerCik = Integer.parseInt(issuerCikValue);

        // Parse filing type
        String typeValue = entry.getCategory().getTerm();
        FilingType type = FilingType.ofValue(typeValue);

        // Parse filing date
        String filedAtValue = summaryParts[2];
        LocalDate filedAt = DateUtils.parse(filedAtValue, "yyyy-MM-dd");

        return new FilingReference(accNo, issuerCik, type, filedAt);
    }
}
