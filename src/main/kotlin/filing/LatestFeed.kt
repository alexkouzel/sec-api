package com.alexkouzel.sec.filing

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.http.attribute
import com.alexkouzel.sec.http.child
import com.alexkouzel.sec.http.children
import com.alexkouzel.sec.http.isoDate
import com.alexkouzel.sec.http.parseXml
import com.alexkouzel.sec.http.text
import org.w3c.dom.Element
import java.net.URLEncoder

// The latest filings feed (Atom). It lists a filing once per related CIK, with the CIK's role
// at the end of the title: "4 - WASTE ENERGY CORP. (0001515139) (Issuer)".
internal object LatestFeed {
    val counts = listOf(10, 20, 40, 80, 100)

    private val accessionInId = Regex("""accession-number=(\d{10}-\d{2}-\d{6})""")
    private val accessionInSummary = Regex("""AccNo:\s*(?:</b>)?\s*(\d{10}-\d{2}-\d{6})""")
    private val filedInSummary = Regex("""Filed:\s*(?:</b>)?\s*(\d{4}-\d{2}-\d{2})""")
    private val cikInLink = Regex("""/data/(\d+)/""")
    private val roleInTitle = Regex("""\(([^()]+)\)\s*$""")

    // Of a filing's entries, these roles name the company the filing is about.
    private val preferredRoles = listOf("Issuer", "Subject")

    fun url(
        count: Int,
        form: FormType?,
    ): String {
        val url =
            "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent" +
                "&start=0&count=$count&output=atom"
        if (form == null) return url
        return url + "&type=" + URLEncoder.encode(form.value, Charsets.UTF_8)
    }

    fun parse(content: ByteArray): List<FilingRef> {
        val listings = parseXml(content).children("entry").map(::listing)
        return listings
            .groupBy { it.ref.accessionNumber }
            .values
            .map { pick(it).ref }
    }

    private fun pick(listings: List<Listing>): Listing {
        for (role in preferredRoles) {
            val listing = listings.find { it.role == role }
            if (listing != null) return listing
        }
        return listings.first()
    }

    private fun listing(entry: Element): Listing {
        val id = entry.text("id")
        val summary = entry.text("summary")
        val link = entry.child("link")?.attribute("href")
        val category = entry.child("category")?.attribute("term")
        val title = entry.text("title")

        val accessionNumber =
            accessionInId.group(id)
                ?: accessionInSummary.group(summary)
                ?: throw missing("accession number", id)
        val cik = cikInLink.group(link) ?: throw missing("CIK", accessionNumber)
        val filedOn = filedInSummary.group(summary) ?: throw missing("filed date", accessionNumber)
        val form = category ?: throw missing("form type", accessionNumber)

        val ref =
            FilingRef(
                accessionNumber = accessionNumber,
                cik = cik.toLong(),
                formType = FormType(form),
                filedOn = isoDate(filedOn),
            )
        return Listing(ref, role = roleInTitle.group(title))
    }

    private fun Regex.group(text: String?): String? {
        if (text == null) return null
        return find(text)?.groupValues?.get(1)
    }

    private fun missing(
        what: String,
        entry: String?,
    ) = SecException.Parse("No $what in latest feed entry $entry")

    private class Listing(
        val ref: FilingRef,
        val role: String?,
    )
}
