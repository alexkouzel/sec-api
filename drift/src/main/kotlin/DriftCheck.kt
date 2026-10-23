package com.alexkouzel.sec.drift

import com.alexkouzel.sec.SecClient
import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.filing.FilingRef
import com.alexkouzel.sec.filing.FormType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset

// Samples recent data from every EDGAR source through the library and records every
// difference from what it expects, instead of stopping at the first. Unknown fields come
// from the library's listener; a failure's raw response is fetched again and saved.
internal class DriftCheck(
    private val userAgent: String,
    private val known: KnownVersions,
    private val report: Report,
) {
    private val unknownFields = linkedMapOf<String, MutableSet<String>>()
    private val sec =
        SecClient(userAgent, unknownFields = { url, field -> unknownFields.getOrPut(url) { sortedSetOf() } += field })
    private val raw = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    fun run() {
        sec.use {
            specPage()
            check("Company tickers") { report.checked("Company tickers", "${sec.companies.all().size} rows") }
            SUBMISSION_CIKS.forEach { cik ->
                check(
                    "Submissions $cik",
                ) { report.checked("Submissions $cik", "${sec.filings.ofCompany(cik).size} filings") }
            }
            check("Latest feed") { report.checked("Latest feed", "${sec.filings.latest().size} filings") }
            check("Latest feed (Form 4)") {
                report.checked("Latest feed (Form 4)", "${sec.filings.latest(form = FormType.F4).size} filings")
            }
            check("Daily list") {
                report.checked("Daily list", "${sec.filings.daily(1).size} filings (none on weekends and holidays)")
            }
            check("Daily list (Form 4)") {
                report.checked("Daily list (Form 4)", "${sec.filings.daily(1, FormType.F4).size} filings")
            }
            ownershipFilings(quarterlyIndexes())
        }
    }

    private fun specPage() {
        val source = "Spec page"
        val html = fetch(SpecPage.URL)?.decodeToString()
        if (html == null) {
            report.finding(source, "request failed", SpecPage.URL)
            return
        }
        val current = SpecPage.currentVersion(html)
        val linked = SpecPage.linkedVersions(html)
        report.checked(
            source,
            "Ownership section says version ${current ?: "?"}, packages linked: ${linked.joinToString()}; " +
                "`specs/` has ${known.ownershipSpec}",
        )
        when {
            current == null -> {
                report.finding(
                    source,
                    "layout changed",
                    "No version found in the Ownership section",
                    "spec-page/technical-specifications.html",
                    html.toByteArray(),
                )
            }

            current != known.ownershipSpec -> {
                report.finding(
                    source,
                    "new spec version",
                    "Ownership spec $current is current; `specs/` has ${known.ownershipSpec}",
                )
            }
        }
        linked
            .filter { SpecPage.compareVersions(it, known.ownershipSpec) > 0 && it != current }
            .forEach { report.finding(source, "new spec version", "Ownership spec package $it is linked") }
    }

    // The current quarter's index, plus the previous one when a form type is short of its sample.
    private fun quarterlyIndexes(): List<FilingRef> {
        val today = LocalDate.now(ZoneOffset.UTC)
        val refs = quarterlyIndex(today)
        val isShort = OWNERSHIP_SAMPLE.any { (form, size) -> refs.count { it.formType == form } < size }
        return if (isShort) refs + quarterlyIndex(today.minusMonths(QUARTER_MONTHS)) else refs
    }

    private fun quarterlyIndex(day: LocalDate): List<FilingRef> {
        val quarter = (day.monthValue - 1) / QUARTER_MONTHS.toInt() + 1
        val source = "Quarterly index ${day.year} Q$quarter"
        return check(
            source,
        ) { sec.filings.quarter(day.year, quarter).also { report.checked(source, "${it.size} filings") } }
            .orEmpty()
    }

    private fun ownershipFilings(index: List<FilingRef>) {
        val schemaVersions = sortedMapOf<String, Int>()
        val sampled =
            OWNERSHIP_SAMPLE.map { (form, size) ->
                val recent =
                    index
                        .filter { it.formType == form }
                        .sortedWith(compareBy<FilingRef>({ it.filedOn }, { it.accessionNumber }).reversed())
                        .take(size)
                recent.forEach { ref -> ownershipFiling(ref, schemaVersions) }
                "${recent.size} × $form"
            }
        report.checked("Ownership filings", "${sampled.joinToString()}; schemaVersions $schemaVersions")
    }

    private fun ownershipFiling(
        ref: FilingRef,
        schemaVersions: MutableMap<String, Int>,
    ) {
        val source = "Ownership ${ref.formType} ${ref.accessionNumber}"
        val filing = check(source) { sec.ownership.get(ref) } ?: return
        val schemaVersion = filing.schemaVersion.toString()
        schemaVersions.merge(schemaVersion, 1, Int::plus)
        if (schemaVersion !in known.schemaVersions) {
            findingWithRaw(
                source,
                "unseen schemaVersion",
                "$schemaVersion is not in `specs/versions.json`",
                ownershipUrl(ref),
            )
        }
        if (filing.formType != ref.formType) {
            findingWithRaw(
                source,
                "form type mismatch",
                "header says ${filing.formType}, index says ${ref.formType}",
                ownershipUrl(ref),
            )
        }
    }

    // Runs one call; a failure and any unknown fields become findings with their raw responses.
    private fun <T> check(
        source: String,
        call: () -> T,
    ): T? {
        unknownFields.clear()
        val result =
            try {
                call()
            } catch (e: SecException) {
                val kind = if (e is SecException.Http) "request failed" else "parse error"
                findingWithRaw(source, kind, e.message.orEmpty(), e.url.takeIf { e is SecException.Parse })
                null
            }
        unknownFields.forEach { (url, fields) -> findingWithRaw(source, "unknown fields", fields.joinToString(), url) }
        return result
    }

    private fun findingWithRaw(
        source: String,
        kind: String,
        detail: String,
        url: String?,
    ) {
        val bytes = url?.let(::fetch)
        if (url == null || bytes == null) {
            report.finding(source, kind, detail)
        } else {
            report.finding(source, kind, detail, rawName(url), bytes)
        }
    }

    private fun fetch(url: String): ByteArray? {
        // Raw requests come on top of the library's; a pause keeps them under EDGAR's limit.
        Thread.sleep(RAW_PAUSE)
        val request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", userAgent).build()
        return runCatching { raw.send(request, HttpResponse.BodyHandlers.ofByteArray()) }
            .getOrNull()
            ?.takeIf { it.statusCode() == HTTP_OK }
            ?.body()
    }

    private fun ownershipUrl(ref: FilingRef) =
        "https://www.sec.gov/Archives/edgar/data/${ref.cik}/${ref.accessionNumber.replace(
            "-",
            "",
        )}/${ref.accessionNumber}.txt"

    private companion object {
        // Apple, Tesla, the fixture's CIK (all with older filings files), and an insider (Tim Cook).
        val SUBMISSION_CIKS = listOf(320193L, 1318605L, 946581L, 1214156L)

        // About 50 recent ownership filings, every form type.
        val OWNERSHIP_SAMPLE =
            with(FormType) { mapOf(F4 to 20, F3 to 10, F5 to 6, F4_A to 6, F3_A to 4, F5_A to 4) }

        const val QUARTER_MONTHS = 3L
        const val HTTP_OK = 200
        val RAW_PAUSE: Duration = Duration.ofMillis(150)
    }
}

// A file name for a response, from its URL's path and query.
internal fun rawName(url: String): String {
    val uri = URI.create(url)
    val name = uri.path.trimStart('/') + (uri.query?.let { "?$it" }.orEmpty())
    return name.replace(Regex("[^A-Za-z0-9._-]+"), "_")
}
