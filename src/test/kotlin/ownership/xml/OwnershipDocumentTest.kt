package com.alexkouzel.sec.ownership.xml

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.ownership.FilingText
import com.alexkouzel.sec.ownership.TransactionCode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.lang.reflect.ParameterizedType
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals

// Checks the XML classes against EDGAR's own definitions: the XSDs in specs/ and the filings
// in the fixtures. When the SEC publishes a new spec, put it in specs/ and run these.
class OwnershipDocumentTest {
    @Test
    fun `every XSD element has a property`() {
        val elements = xsdElements() - "ownershipDocument"

        val missing = elements - properties(OwnershipDocument::class.java)

        assertEquals(emptySet(), missing, "XSD elements without a property in ownership/xml")
    }

    @Test
    fun `every XSD transaction code is known`() {
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
        val dom =
            dom(
                text
                    .decodeToString()
                    .substringAfter(
                        "<XML>",
                    ).substringBefore("</XML>")
                    .trim()
                    .toByteArray(),
            )

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

    // The element names of every ownership XSD in the newest spec.
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

    // Property names of a class in ownership/xml and of every class it holds, named as in the XSD.
    private fun properties(type: Class<*>): Set<String> {
        val names = mutableSetOf<String>()
        var current: Class<*>? = type
        while (current != null && current.packageName == type.packageName) {
            for (field in current.declaredFields) {
                names += field.name
                val held =
                    (field.genericType as? ParameterizedType)?.actualTypeArguments?.first()
                        ?: field.type
                if (held is Class<*> && held.packageName == type.packageName && held != type) {
                    names += properties(held)
                }
            }
            current = current.superclass
        }
        return names
    }

    // The newest spec in specs/, e.g. specs/ownership-xml-v5.5.
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
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        return factory.newDocumentBuilder().parse(bytes.inputStream())
    }

    private fun Document.count(element: String) = getElementsByTagName(element).length

    private companion object {
        const val XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"

        @JvmStatic
        fun fixtures(): List<String> =
            Path
                .of("src/test/resources/edgar/ownership")
                .listDirectoryEntries("*.txt")
                .map { it.name.removeSuffix(".txt") }
                .sorted()
    }
}
