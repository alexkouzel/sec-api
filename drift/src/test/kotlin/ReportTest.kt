package com.alexkouzel.sec.drift

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportTest {
    private val date = LocalDate.parse("2026-10-27")

    @Test
    fun `a clean run says so`(
        @TempDir dir: Path,
    ) {
        val report = Report(dir, date)
        report.checked("Latest feed", "40 filings")

        report.write()

        assertEquals("0", Files.readString(dir.resolve("findings-count")))
        assertEquals(
            "# EDGAR drift check, 2026-10-27\n\nNo drift found.\n\n## Checked\n\n- Latest feed: 40 filings\n",
            Files.readString(dir.resolve("report.md")),
        )
    }

    @Test
    fun `findings are listed with their raw responses`(
        @TempDir dir: Path,
    ) {
        val raw = "<xml>café</xml>".toByteArray()
        val report = Report(dir, date)
        report.finding("Ownership 4 x", "unknown fields", "a|b\nc", "ownership/x.txt", raw)
        report.finding("Spec page", "new spec version", "5.6")

        report.write()

        assertEquals("2", Files.readString(dir.resolve("findings-count")))
        assertContentEquals(raw, Files.readAllBytes(dir.resolve("raw/ownership/x.txt")))
        val markdown = Files.readString(dir.resolve("report.md"))
        assertContains(markdown, "| Ownership 4 x | unknown fields | a\\|b c | `raw/ownership/x.txt` |\n")
        assertContains(markdown, "| Spec page | new spec version | 5.6 |  |\n")
    }

    @Test
    fun `the issue is shortened to fit GitHub`(
        @TempDir dir: Path,
    ) {
        val report = Report(dir, date)
        repeat(160) { report.finding("Source $it", "parse error", "x".repeat(1_000)) }

        report.write()

        val issue = Files.readString(dir.resolve("issue.md"))
        assertTrue(issue.length < 65_536, issue.length.toString())
        assertContains(issue, "10 more in the artifact's `report.md`.")
        assertContains(Files.readString(dir.resolve("report.md")), "Source 159")
    }
}
