package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.map
import com.alexkouzel.sec.http.xml
import java.net.URLEncoder
import java.time.LocalDate

internal object LatestFeed {
    val counts = listOf(10, 20, 40, 80, 100)

    private val accessionInId = Regex("""accession-number=(\d{10}-\d{2}-\d{6})""")
    private val accessionInSummary = Regex("""AccNo:\s*(?:</b>)?\s*(\d{10}-\d{2}-\d{6})""")
    private val filedInSummary = Regex("""Filed:\s*(?:</b>)?\s*(\d{4}-\d{2}-\d{2})""")
    private val cikInLink = Regex("""/data/(\d+)/""")
    private val roleInTitle = Regex("""\(([^()]+)\)\s*$""")

    // The feed lists a filing once per related CIK; these roles name the company it is about.
    private val preferredRoles = listOf("Issuer", "Subject")

    fun url(
        start: Int,
        count: Int,
        form: FormType?,
    ): String {
        val url = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&start=$start&count=$count&output=atom"
        return if (form == null) url else url + "&type=" + URLEncoder.encode(form.value, Charsets.UTF_8)
    }

    fun parse(content: ByteArray): List<FilingRef> =
        xml
            .map<Feed>(content)
            .entry
            .map(::listing)
            .groupBy { it.ref.accessionNumber }
            .values
            .map { listings ->
                preferredRoles.firstNotNullOfOrNull { role -> listings.find { it.role == role } } ?: listings.first()
            }.map { it.ref }

    private fun listing(entry: Entry): Listing {
        val summary = entry.summary.orEmpty()
        val accessionNumber =
            (accessionInId.group(entry.id) ?: accessionInSummary.group(summary)).required("accession number", entry.id)
        val cik = cikInLink.group(entry.link?.href).required("CIK in the link", accessionNumber)
        val filedOn = filedInSummary.group(summary).required("filed date in the summary", accessionNumber)
        val form = entry.category?.term.required("form type", accessionNumber)
        val ref = FilingRef(accessionNumber, cik.toLong(), FormType(form.trim()), LocalDate.parse(filedOn))
        return Listing(ref, roleInTitle.group(entry.title?.trim()))
    }

    private fun Regex.group(text: String?): String? = text?.let { find(it) }?.groupValues?.get(1)

    private fun String?.required(
        what: String,
        entry: String?,
    ): String = this ?: throw SecException.Parse("No $what of latest feed entry $entry")

    private class Listing(
        val ref: FilingRef,
        val role: String?,
    )

    internal data class Feed(
        val entry: List<Entry> = emptyList(),
    )

    internal data class Entry(
        val title: String? = null,
        val summary: String? = null,
        val id: String? = null,
        val link: Link? = null,
        val category: Category? = null,
    )

    internal data class Link(
        val href: String? = null,
    )

    internal data class Category(
        val term: String? = null,
    )
}
