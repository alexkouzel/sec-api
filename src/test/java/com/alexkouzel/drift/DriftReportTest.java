package com.alexkouzel.drift;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DriftReportTest {
    private static final LocalDate DATE = LocalDate.parse("2026-10-27");

    @Test
    public void cleanRun(@TempDir Path dir) throws Exception {
        DriftReport report = new DriftReport(dir, DATE);
        report.checked("Latest feed", "40 filings");
        report.write();

        assertEquals("0", Files.readString(dir.resolve("findings-count")));
        assertEquals("""
                # EDGAR drift check, 2026-10-27

                No drift found.

                ## Checked

                - Latest feed: 40 filings
                """, Files.readString(dir.resolve("report.md")));
    }

    @Test
    public void findingsWithRawResponses(@TempDir Path dir) throws Exception {
        byte[] raw = "<xml>café</xml>".getBytes(StandardCharsets.UTF_8);
        DriftReport report = new DriftReport(dir, DATE);
        report.finding("Ownership 4 x", "unknown fields", "a|b\nc", "ownership/x.txt", raw);
        report.finding("Spec page", "new spec version", "5.6");
        report.write();

        assertEquals("2", Files.readString(dir.resolve("findings-count")));
        assertArrayEquals(raw, Files.readAllBytes(dir.resolve("raw/ownership/x.txt")));
        String md = Files.readString(dir.resolve("report.md"));
        assertTrue(md.contains("| Ownership 4 x | unknown fields | a\\|b c | `raw/ownership/x.txt` |\n"), md);
        assertTrue(md.contains("| Spec page | new spec version | 5.6 |  |\n"), md);
    }

    @Test
    public void issueIsShortened(@TempDir Path dir) throws Exception {
        DriftReport report = new DriftReport(dir, DATE);
        for (int i = 0; i < 160; i++) {
            report.finding("Source " + i, "parse error", "x".repeat(1_000));
        }
        report.write();

        String issue = Files.readString(dir.resolve("issue.md"));
        assertTrue(issue.length() < 65_536, String.valueOf(issue.length()));
        assertTrue(issue.contains("10 more in the artifact's `report.md`."));
        assertTrue(Files.readString(dir.resolve("report.md")).contains("Source 159"));
    }
}
