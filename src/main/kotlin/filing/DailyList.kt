package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The daily list (cgi-bin/current): one row per filing and related CIK, between two <hr> tags.
// 10-21-2026   <a href="/Archives/edgar/data/1553209/0001099910-26-000286-index.html">4</a> ...
internal object DailyList {
    private const val RULE = "<hr>"

    private val row =
        Regex(
            """^(\d{2}-\d{2}-\d{4})\s+""" +
                """<a href="/Archives/edgar/data/(\d+)/(\d{10}-\d{2}-\d{6})-index\.html?">""" +
                """([^<]+)</a>""",
        )
    private val date = DateTimeFormatter.ofPattern("MM-dd-yyyy")

    fun url(
        daysAgo: Int,
        formType: FormType?,
    ): String {
        val url = "https://www.sec.gov/cgi-bin/current?q1=$daysAgo"
        if (formType == null) return url
        return url + "&q3=" + URLEncoder.encode(formType.value, Charsets.UTF_8)
    }

    fun parse(content: ByteArray): List<FilingRef> {
        val page = content.decodeToString()
        val start = page.indexOf(RULE)
        val end = page.indexOf(RULE, start + RULE.length)
        if (start == -1 || end == -1) {
            throw SecException.Parse("No list between two $RULE tags in the daily list")
        }

        val rows = page.substring(start + RULE.length, end).lines().filter { it.isNotBlank() }
        return rows.map(::ref).distinctBy { it.accessionNumber }
    }

    private fun ref(line: String): FilingRef {
        val match =
            row.find(line.trim()) ?: throw SecException.Parse("Unrecognized daily list row: $line")
        val (filedOn, cik, accessionNumber, form) = match.destructured
        return FilingRef(
            accessionNumber = accessionNumber,
            cik = cik.toLong(),
            formType = FormType(form.trim()),
            filedOn = LocalDate.parse(filedOn, date),
        )
    }
}
