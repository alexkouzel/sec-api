package com.alexkouzel.sec.drift

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

// Findings of one run, written as report.md with each offending raw response under raw/
// (candidate new fixtures). For the scheduled workflow it also writes issue.md (the report,
// shortened to fit a GitHub issue) and the finding count in findings-count.
internal class Report(
    private val dir: Path,
    private val date: LocalDate,
) {
    data class Finding(
        val source: String,
        val kind: String,
        val detail: String,
        val raw: String? = null,
    )

    private val found = mutableListOf<Finding>()
    private val checked = mutableListOf<String>()

    val findings: List<Finding> get() = found.toList()

    fun checked(
        source: String,
        summary: String,
    ) {
        checked += "$source: $summary"
    }

    fun finding(
        source: String,
        kind: String,
        detail: String,
    ) {
        found += Finding(source, kind, detail)
    }

    // Saves the response the finding came from as raw/<rawName>.
    fun finding(
        source: String,
        kind: String,
        detail: String,
        rawName: String,
        raw: ByteArray,
    ) {
        val file = dir.resolve("raw").resolve(rawName)
        Files.createDirectories(file.parent)
        Files.write(file, raw)
        found += Finding(source, kind, detail, "raw/$rawName")
    }

    fun write() {
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("report.md"), markdown(Int.MAX_VALUE))
        Files.writeString(dir.resolve("issue.md"), markdown(ISSUE_ROWS))
        Files.writeString(dir.resolve("findings-count"), found.size.toString())
    }

    fun markdown(maxRows: Int): String =
        buildString {
            append("# EDGAR drift check, $date\n\n")
            if (found.isEmpty()) {
                append("No drift found.\n")
            } else {
                append("${found.size} ${if (found.size == 1) "finding" else "findings"}. ")
                append("Raw responses are in the run's `drift-report` artifact; see the runbook in `CLAUDE.md`.\n\n")
                append("| Source | Kind | Detail | Raw |\n|--------|------|--------|-----|\n")
                found.take(maxRows).forEach {
                    append(
                        "| ${cell(it.source)} | ${cell(it.kind)} | ${cell(it.detail)} | ${it.raw?.let { raw ->
                            "`$raw`"
                        }.orEmpty()} |\n",
                    )
                }
                if (found.size > maxRows) append("\n${found.size - maxRows} more in the artifact's `report.md`.\n")
            }
            append("\n## Checked\n\n")
            checked.forEach { append("- $it\n") }
        }

    private fun cell(value: String): String {
        val text = value.replace("\r", "").replace("\n", " ").replace("|", "\\|")
        return if (text.length > MAX_DETAIL) text.take(MAX_DETAIL) + "…" else text
    }

    private companion object {
        // A GitHub issue body holds at most 65,536 characters.
        const val ISSUE_ROWS = 150
        const val MAX_DETAIL = 300
    }
}
