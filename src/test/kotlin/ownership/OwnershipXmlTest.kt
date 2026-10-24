package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.filing.FormType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OwnershipXmlTest {
    @Test
    fun `reads every element the XSD defines`() {
        val quotedName = Regex(""""([A-Za-z0-9]+)"""")
        val named = quotedName.findAll(parserSource()).map { it.groupValues[1] }.toSet()

        val missing = xsdElements() - named - unread

        assertEquals(emptySet(), missing, "XSD elements the parser doesn't read")
    }

    @Test
    fun `knows every transaction code the XSD defines`() {
        val common = spec().resolve("schemas/ownershipDocumentCommon.xsd.xml").readText()
        val picklist =
            Regex(
                """name="TRANS_CODE_PICKLIST".*?<xs:pattern value="([^"]+)"""",
                RegexOption.DOT_MATCHES_ALL,
            )
        val codes =
            picklist
                .find(common)
                ?.groupValues
                ?.get(1)
                ?.split('|')
                .orEmpty()

        val unknown = codes.filter { TransactionCode.of(it) == null }

        assertEquals(18, codes.size)
        assertEquals(emptyList(), unknown, "XSD transaction codes missing from TransactionCode")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    fun `keeps every repeated element`(accessionNumber: String) {
        val text = Fixtures.bytes("ownership/$accessionNumber.txt")
        val xml =
            text
                .decodeToString()
                .substringAfter("<XML>")
                .substringBefore("</XML>")
                .trim()
        val dom = dom(xml.toByteArray())

        val filing = FilingText.parse(text)

        val expected =
            mapOf(
                "reportingOwner" to dom.count("reportingOwner"),
                "ownerSignature" to dom.count("ownerSignature"),
                "footnote" to dom.count("footnote"),
                "transaction" to
                    dom.count("nonDerivativeTransaction") + dom.count("derivativeTransaction"),
                "holding" to dom.count("nonDerivativeHolding") + dom.count("derivativeHolding"),
            )
        val actual =
            mapOf(
                "reportingOwner" to filing.owners.size,
                "ownerSignature" to filing.signatures.size,
                "footnote" to filing.footnotes.size,
                "transaction" to filing.transactions.size,
                "holding" to filing.holdings.size,
            )
        assertEquals(expected, actual)
    }

    private fun filing(accessionNumber: String) =
        FilingText.parse(Fixtures.bytes("ownership/$accessionNumber.txt"))

    private fun date(value: String) = LocalDate.parse(value)

    @Test
    fun `form 4 with a sale and indirect holdings`() {
        val filing = filing("0001127602-23-028345")

        assertEquals("NSP", filing.issuer.ticker)
        assertEquals(date("2023-11-27"), filing.periodOfReport)
        assertEquals(false, filing.isRule10b5One)

        val owner = filing.owners.single()
        assertEquals(1055610, owner.cik)
        assertEquals("RAWSON RICHARD G", owner.name)
        assertEquals("KINGWOOD", owner.address?.city)
        assertTrue(owner.relationship.isDirector)
        assertFalse(owner.relationship.isOfficer)

        val sale = filing.transactions.single()
        assertNull(sale.derivative)
        assertEquals(Footnoted("Common Stock"), sale.securityTitle)
        assertEquals(Footnoted(date("2023-11-27")), sale.date)
        assertEquals(TransactionCode.SALE, sale.coding?.code)
        assertEquals(false, sale.coding?.isEquitySwap)
        assertEquals(Footnoted(BigDecimal("3500")), sale.shares)
        assertEquals(Footnoted(BigDecimal("114.36"), listOf("F1")), sale.pricePerShare)
        assertEquals(Footnoted("D"), sale.acquiredOrDisposed)
        assertEquals(Footnoted(BigDecimal("1925")), sale.sharesOwnedAfter)
        assertEquals(Footnoted("D"), sale.directOrIndirect)
        assertNull(sale.timeliness)

        assertEquals(listOf("F1"), filing.footnotes.map { it.id })
        assertTrue(
            filing.footnotes
                .single()
                .text
                .startsWith("Weighted average price."),
        )
        assertEquals(
            "/s/ Christian P. Callens, by Power of Attorney",
            filing.signatures.single().name,
        )
        assertEquals(date("2023-11-28"), filing.signatures.single().date)
    }

    // This filing interleaves holdings and a transaction (holding x3, transaction, holding),
    // which the XSD allows.
    @Test
    fun `keeps every interleaved holding`() {
        val filing = filing("0001127602-23-028345")

        assertEquals(4, filing.holdings.size)
        val holding = filing.holdings.first()
        assertEquals(Footnoted(BigDecimal("84216")), holding.sharesOwned)
        assertEquals(Footnoted("I"), holding.directOrIndirect)
        assertEquals(
            "By Self, as General Partner of POM Services, LLC, " +
                "which is the General Partner of R&D Rawson, LP",
            holding.natureOfOwnership?.value,
        )
    }

    @Test
    fun `form 4 with an option exercise`() {
        val filing = filing("0001701247-26-000014")

        assertEquals(true, filing.isRule10b5One)
        val relationship = filing.owners.first().relationship
        assertFalse(relationship.isDirector)
        assertTrue(relationship.isOfficer)
        assertEquals("CHIEF MEDICAL OFFICER", relationship.officerTitle)

        val (stock, options) = filing.transactions.partition { it.derivative == null }
        assertEquals(2, stock.size)
        val exercise = stock.first()
        assertEquals(TransactionCode.OPTION_EXERCISE, exercise.coding?.code)
        assertEquals(BigDecimal("5000"), exercise.shares?.value)
        assertEquals(BigDecimal("2.48"), exercise.pricePerShare?.value)
        assertEquals("A", exercise.acquiredOrDisposed?.value)
        assertEquals(BigDecimal("30000"), exercise.sharesOwnedAfter?.value)

        val option = options.single()
        assertEquals("Stock Option (right to buy)", option.securityTitle?.value)
        assertEquals(BigDecimal("2.48"), option.derivative?.exercisePrice?.value)
        assertEquals(date("2026-10-19"), option.date?.value)
        assertEquals(TransactionCode.OPTION_EXERCISE, option.coding?.code)
        assertEquals(BigDecimal("5000"), option.shares?.value)
        assertEquals(BigDecimal("0"), option.pricePerShare?.value)
        assertEquals("D", option.acquiredOrDisposed?.value)
        assertEquals(date("2031-06-17"), option.derivative?.expirationDate?.value)
        assertEquals("Common Stock", option.derivative?.underlyingTitle?.value)
        assertEquals(BigDecimal("5000"), option.derivative?.underlyingShares?.value)
        assertEquals(BigDecimal("101268"), option.sharesOwnedAfter?.value)

        assertEquals(listOf("F1", "F2", "F3", "F4", "F5"), filing.footnotes.map { it.id })
    }

    @Test
    fun `form 3 with many owners and holdings`() {
        val filing = filing("0000921895-26-002730")

        assertEquals("Foghorn Therapeutics Inc.", filing.issuer.name)
        assertEquals(date("2026-10-01"), filing.periodOfReport)
        assertFalse(filing.ownsNoSecurities)

        assertEquals(10, filing.owners.size)
        val owner = filing.owners.first()
        assertEquals(1055947, owner.cik)
        assertEquals("BVF PARTNERS L P/IL", owner.name)
        assertTrue(owner.relationship.isTenPercentOwner)
        assertEquals(false, owner.address?.isNonUS)

        assertEquals(emptyList(), filing.transactions)
        val (stock, warrants) = filing.holdings.partition { it.derivative == null }
        assertEquals(3, stock.size)
        assertEquals(BigDecimal("4494462"), stock.first().sharesOwned?.value)
        assertEquals(9, warrants.size)
        val warrant = warrants.first()
        assertEquals("Pre-Funded Warrants to Purchase Common Stock", warrant.securityTitle?.value)
        assertEquals("Common Stock, $0.0001 par value", warrant.derivative?.underlyingTitle?.value)
        assertEquals(BigDecimal("3132723"), warrant.derivative?.underlyingShares?.value)

        assertEquals(
            listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7"),
            filing.footnotes.map { it.id },
        )
    }

    @Test
    fun `form 3 with no securities owned`() {
        val filing = filing("0000929638-26-003820")

        assertTrue(filing.ownsNoSecurities)
        assertEquals(emptyList(), filing.transactions)
        assertEquals(emptyList(), filing.holdings)
        assertEquals(emptyList(), filing.footnotes)
        val relationship = filing.owners.first().relationship
        assertTrue(relationship.isOther)
        assertFalse(relationship.isDirector)
    }

    @Test
    fun `form 3 amendment with an option holding`() {
        val filing = filing("0001193125-26-427843")

        assertEquals(FormType.F3_A, filing.formType)
        assertEquals(date("2026-09-03"), filing.dateOfOriginalSubmission)
        assertEquals(
            "Chief Technology Officer",
            filing.owners
                .first()
                .relationship.officerTitle,
        )

        val option = filing.holdings.first { it.derivative != null }
        assertEquals("Stock Option (right to buy)", option.securityTitle?.value)
        assertEquals(BigDecimal("0.47"), option.derivative?.exercisePrice?.value)
        assertEquals(date("2034-09-09"), option.derivative?.expirationDate?.value)
        assertEquals(BigDecimal("3217784"), option.derivative?.underlyingShares?.value)
    }

    @Test
    fun `form 4 with a non-US owner`() {
        val filing = filing("0001493152-26-046274")

        val address = filing.owners.first().address
        assertEquals(true, address?.isNonUS)
        assertEquals("F4", address?.country)
        assertEquals("BEIJING", address?.city)

        val purchase = filing.transactions.first()
        assertEquals("American depositary shares", purchase.securityTitle?.value)
        assertEquals(TransactionCode.PURCHASE, purchase.coding?.code)
        assertEquals(BigDecimal("5505"), purchase.shares?.value)
        assertEquals(BigDecimal("1.2128"), purchase.pricePerShare?.value)
        assertNull(purchase.deemedExecutionDate)
    }

    @Test
    fun `a footnote can stand in for a value`() {
        val filing = filing("0001887616-26-000024")

        assertEquals(date("2026-10-02"), filing.dateOfOriginalSubmission)
        assertEquals(6, filing.transactions.count { it.derivative == null })
        assertEquals(3, filing.transactions.count { it.derivative != null })
        val first = filing.transactions.first()
        assertEquals(Footnoted(BigDecimal("33967")), first.shares)
        assertEquals(Footnoted(null, listOf("F1")), first.pricePerShare)
    }

    @Test
    fun `keeps several footnotes on one value`() {
        val filing = filing("0001000045-22-000004")

        assertEquals(
            Footnoted(BigDecimal("2262"), listOf("F1", "F2")),
            filing.transactions.first().shares,
        )
    }

    @Test
    fun `keeps footnotes on a transaction coding`() {
        val coding = filing("0001315098-26-000186").transactions.first().coding

        assertEquals(TransactionCode.SALE, coding?.code)
        assertEquals(listOf("F1"), coding?.footnoteIds)
    }

    @Test
    fun `form 5 with fractional shares`() {
        val filing = filing("0001225208-26-008370")

        assertTrue(
            filing.owners
                .first()
                .relationship.isOther,
        )
        assertEquals(12, filing.transactions.size)
        val first = filing.transactions.first()
        assertEquals(date("2025-10-30"), first.date?.value)
        assertEquals(TransactionCode.OTHER, first.coding?.code)
        assertEquals(BigDecimal("24.4000"), first.shares?.value)
        assertEquals(BigDecimal("20.6800"), first.pricePerShare?.value)
        assertEquals(BigDecimal("3794.9080"), first.sharesOwnedAfter?.value)
    }

    @Test
    fun `keeps line breaks in footnote text`() {
        val footnote = filing("0001437749-22-017271").footnotes.single { it.id == "F3" }

        assertContains(footnote.text, "has the effect of\ncorrecting the Form 4")
    }

    @Test
    fun `form 5 amendment with a gift`() {
        val filing = filing("0001437749-22-017271")

        assertEquals(date("2022-06-14"), filing.dateOfOriginalSubmission)
        val gift = filing.transactions.first()
        assertEquals(TransactionCode.GIFT, gift.coding?.code)
        assertEquals(date("2021-09-29"), gift.date?.value)
        assertEquals(BigDecimal("2000"), gift.shares?.value)
    }

    // The parser's source, whose string literals name the elements it reads.
    private fun parserSource(): String {
        val dir = Path.of("src/main/kotlin/ownership")
        return dir.resolve("OwnershipXml.kt").readText() + dir.resolve("XmlValues.kt").readText()
    }

    // The element names of every ownership XSD in the newest spec in specs/.
    private fun xsdElements(): Set<String> {
        val xsds = spec().resolve("schemas").listDirectoryEntries("ownership*")
        val names = mutableSetOf<String>()
        for (xsd in xsds) {
            val elements = dom(xsd.readBytes()).getElementsByTagNameNS(XML_SCHEMA, "element")
            for (i in 0 until elements.length) {
                names += (elements.item(i) as Element).getAttribute("name")
            }
        }
        return names - ""
    }

    private fun spec(): Path {
        val specs = Path.of("specs").listDirectoryEntries("ownership-xml-v*")
        return specs.maxBy { version(it.name.removePrefix("ownership-xml-v")) }
    }

    private fun version(text: String): Int {
        val (major, minor) = text.split('.').map(String::toInt)
        return major * 1000 + minor
    }

    private fun dom(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newDocumentBuilder().parse(bytes.inputStream())
    }

    private fun Document.count(element: String) = getElementsByTagName(element).length

    private companion object {
        const val XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"

        // Elements the model leaves out on purpose: the form type is in the header, the other
        // two are EDGAR's internal codes.
        val unread = setOf("documentType", "rptOwnerCcc", "rptOwnerGoodAddress")

        @JvmStatic
        fun fixtures(): List<String> =
            Path
                .of("src/test/resources/edgar/ownership")
                .listDirectoryEntries("*.txt")
                .map { it.name.removeSuffix(".txt") }
                .sorted()
    }
}
