package com.alexkouzel.drift;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Findings of one drift check run, written as {@code report.md} with each offending raw
 * response under {@code raw/} (candidate new fixtures). For the scheduled workflow it also
 * writes {@code issue.md} (the report, shortened to fit a GitHub issue) and the finding
 * count in {@code findings-count}.
 */
public class DriftReport {
    // A GitHub issue body holds at most 65,536 characters.
    private static final int MAX_ROWS = 150;
    private static final int MAX_DETAIL = 300;

    public record Finding(String source, String kind, String detail, String raw) {
    }

    private final Path dir;
    private final LocalDate date;
    private final List<Finding> findings = new ArrayList<>();
    private final List<String> checked = new ArrayList<>();

    public DriftReport(Path dir, LocalDate date) {
        this.dir = dir;
        this.date = date;
    }

    public List<Finding> findings() {
        return findings;
    }

    /** Notes what a source was checked with, for the report's "Checked" list. */
    public void checked(String source, String summary) {
        checked.add(source + ": " + summary);
    }

    public void finding(String source, String kind, String detail) {
        findings.add(new Finding(source, kind, detail, null));
    }

    /** Records a finding and saves the response it came from as {@code raw/<rawName>}. */
    public void finding(String source, String kind, String detail, String rawName, byte[] raw) throws IOException {
        Path file = dir.resolve("raw").resolve(rawName);
        Files.createDirectories(file.getParent());
        Files.write(file, raw);
        findings.add(new Finding(source, kind, detail, "raw/" + rawName));
    }

    public void write() throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("report.md"), toMarkdown(Integer.MAX_VALUE), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("issue.md"), toMarkdown(MAX_ROWS), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("findings-count"), String.valueOf(findings.size()));
    }

    public String toMarkdown(int maxRows) {
        StringBuilder md = new StringBuilder();
        md.append("# EDGAR drift check, ").append(date).append("\n\n");
        if (findings.isEmpty()) {
            md.append("No drift found.\n");
        } else {
            md.append(findings.size()).append(findings.size() == 1 ? " finding" : " findings")
                    .append(". Raw responses are in the run's `drift-report` artifact; ")
                    .append("see the runbook in `CLAUDE.md`.\n\n")
                    .append("| Source | Kind | Detail | Raw |\n")
                    .append("|--------|------|--------|-----|\n");
            for (Finding finding : findings.subList(0, Math.min(maxRows, findings.size()))) {
                md.append("| ").append(cell(finding.source()))
                        .append(" | ").append(cell(finding.kind()))
                        .append(" | ").append(cell(finding.detail()))
                        .append(" | ").append(finding.raw() == null ? "" : "`" + finding.raw() + "`")
                        .append(" |\n");
            }
            if (findings.size() > maxRows) {
                md.append("\n").append(findings.size() - maxRows).append(" more in the artifact's `report.md`.\n");
            }
        }
        md.append("\n## Checked\n\n");
        checked.forEach(line -> md.append("- ").append(line).append("\n"));
        return md.toString();
    }

    private static String cell(String value) {
        String text = value.replace("\r", "").replace("\n", " ").replace("|", "\\|");
        return text.length() > MAX_DETAIL ? text.substring(0, MAX_DETAIL) + "…" : text;
    }
}
