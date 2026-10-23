package com.alexkouzel.drift;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Ownership spec versions from https://www.sec.gov/submit-filings/technical-specifications.
 */
public final class SpecWatcher {
    public static final String URL = "https://www.sec.gov/submit-filings/technical-specifications";
    public static final Comparator<String> VERSION_ORDER = SpecWatcher::compareVersions;

    private static final String SECTION = "id=\"ownership_forms\"";
    private static final Pattern SECTION_VERSION = Pattern.compile(
            "Version</strong>(?:\\s|&nbsp;)*(\\d+(?:\\.\\d+)*)");
    // Every spec package link, drafts included (e.g. ...-v5-6_d.zip).
    private static final Pattern PACKAGE = Pattern.compile("ownershipxmltechspec-v(\\d+(?:-\\d+)*)(_d)?\\.zip");

    private SpecWatcher() {
    }

    /** Version in the Ownership section's heading, or empty if the page layout changed. */
    public static Optional<String> currentVersion(String html) {
        int section = html.indexOf(SECTION);
        if (section == -1) {
            return Optional.empty();
        }
        Matcher matcher = SECTION_VERSION.matcher(html);
        return matcher.find(section) && matcher.start() - section < 1_000
                ? Optional.of(matcher.group(1))
                : Optional.empty();
    }

    /** Versions of every Ownership spec package linked from the page, drafts marked "(draft)". */
    public static Set<String> linkedVersions(String html) {
        Set<String> versions = new TreeSet<>();
        Matcher matcher = PACKAGE.matcher(html);
        while (matcher.find()) {
            versions.add(matcher.group(1).replace('-', '.') + (matcher.group(2) != null ? " (draft)" : ""));
        }
        return versions;
    }

    /** Compares dotted versions numerically; a " (draft)" suffix is ignored. */
    public static int compareVersions(String a, String b) {
        String[] left = a.replace(" (draft)", "").split("\\.");
        String[] right = b.replace(" (draft)", "").split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? Integer.parseInt(left[i]) : 0;
            int r = i < right.length ? Integer.parseInt(right[i]) : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }
}
