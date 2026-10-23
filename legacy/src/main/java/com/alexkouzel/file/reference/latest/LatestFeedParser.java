package com.alexkouzel.file.reference.latest;

import com.alexkouzel.common.exceptions.ParsingException;
import com.alexkouzel.common.utils.DateUtils;
import com.alexkouzel.file.FileType;
import com.alexkouzel.file.reference.FileRef;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the latest filings Atom feed.
 * <p>
 * The feed lists a filing once per related CIK, with the role in the title, e.g.
 * "4 - Uxin Ltd (0001729173) (Issuer)". Each filing is returned once, in feed order,
 * with the CIK of its Issuer or Subject entry, or else of its first entry.
 */
@UtilityClass
public class LatestFeedParser {
    private final Pattern CIK_PATTERN = Pattern.compile("/data/(\\d+)/");
    private final Pattern ACC_NO_ID_PATTERN = Pattern.compile("accession-number=(\\d{10}-\\d{2}-\\d{6})");
    private final Pattern ACC_NO_SUMMARY_PATTERN = Pattern.compile("AccNo:\\s*(?:</b>)?\\s*(\\d{10}-\\d{2}-\\d{6})");
    private final Pattern FILED_PATTERN = Pattern.compile("Filed:\\s*(?:</b>)?\\s*(\\d{4}-\\d{2}-\\d{2})");
    private final Pattern ROLE_PATTERN = Pattern.compile("\\(([^()]+)\\)\\s*$");
    private final List<String> PREFERRED_ROLES = List.of("Issuer", "Subject");

    private record Entry(FileRef ref, String role) {
    }

    public List<FileRef> parse(LatestFeed feed) throws ParsingException {
        Map<String, List<Entry>> byAccNo = new LinkedHashMap<>();
        if (feed.getEntry() != null) {
            for (LatestFeedEntry feedEntry : feed.getEntry()) {
                Entry entry = parseEntry(feedEntry);
                byAccNo.computeIfAbsent(entry.ref().accNo(), k -> new ArrayList<>()).add(entry);
            }
        }
        List<FileRef> fileRefs = new ArrayList<>();
        for (List<Entry> entries : byAccNo.values()) {
            fileRefs.add(pick(entries).ref());
        }
        return fileRefs;
    }

    private Entry pick(List<Entry> entries) {
        for (String role : PREFERRED_ROLES) {
            for (Entry entry : entries) {
                if (role.equals(entry.role())) {
                    return entry;
                }
            }
        }
        return entries.getFirst();
    }

    private Entry parseEntry(LatestFeedEntry entry) throws ParsingException {
        String summary = entry.getSummary() == null ? "" : entry.getSummary();

        String accNo = find(ACC_NO_ID_PATTERN, entry.getId());
        if (accNo == null) {
            accNo = find(ACC_NO_SUMMARY_PATTERN, summary);
        }
        if (accNo == null) {
            throw new ParsingException("No accession number in entry: " + entry.getId());
        }

        String cikValue = entry.getLink() == null ? null : find(CIK_PATTERN, entry.getLink().getHref());
        if (cikValue == null) {
            throw new ParsingException("No CIK in entry link: " + accNo);
        }

        String filedAtValue = find(FILED_PATTERN, summary);
        if (filedAtValue == null) {
            throw new ParsingException("No filing date in entry summary: " + accNo);
        }

        if (entry.getCategory() == null || entry.getCategory().getTerm() == null) {
            throw new ParsingException("No form type in entry: " + accNo);
        }
        FileType type = FileType.ofValue(entry.getCategory().getTerm());
        LocalDate filedAt = DateUtils.parse(filedAtValue, "yyyy-MM-dd");
        String role = find(ROLE_PATTERN, entry.getTitle() == null ? "" : entry.getTitle().trim());

        return new Entry(new FileRef(accNo, Integer.parseInt(cikValue), type, filedAt), role);
    }

    private String find(Pattern pattern, String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
