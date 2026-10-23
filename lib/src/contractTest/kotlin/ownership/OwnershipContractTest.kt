package com.alexkouzel.sec.ownership

import com.alexkouzel.sec.Fixtures
import com.alexkouzel.sec.http.xml
import com.alexkouzel.sec.ownership.xml.OwnershipDocument
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3c.dom.Document
import org.w3c.dom.Element
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JavaType
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// Checks ownership/xml and the model against EDGAR's own definitions: the XSDs in specs/ and
// the filings in the fixtures. Parsing is strict here, unlike in the library, so new or
// unmapped elements are noticed.
class OwnershipContractTest {
    private val strict = xml.rebuild().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    fun `every fixture parses strictly`(accessionNumber: String) {
        val block = xmlBlock(accessionNumber)

        assertNotNull(strict.readValue(block, OwnershipDocument::class.java).issuer)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    fun `the model keeps every repeated element`(accessionNumber: String) {
        val dom = dom(xmlBlock(accessionNumber).toByteArray())
        val filing = FilingText.parse(Fixtures.bytes("ownership/$accessionNumber.txt"))

        val (stockTransactions, derivativeTransactions) = filing.transactions.partition { it.derivative == null }
        val (stockHoldings, derivativeHoldings) = filing.holdings.partition { it.derivative == null }
        val counts =
            mapOf(
                "reportingOwner" to filing.owners.size,
                "ownerSignature" to filing.signatures.size,
                "footnote" to filing.footnotes.size,
                "nonDerivativeTransaction" to stockTransactions.size,
                "derivativeTransaction" to derivativeTransactions.size,
                "nonDerivativeHolding" to stockHoldings.size,
                "derivativeHolding" to derivativeHoldings.size,
                "footnoteId" to footnoteRefs(filing),
            )

        assertEquals(counts.mapValues { (element, _) -> dom.getElementsByTagName(element).length }, counts)
    }

    @Test
    fun `every XSD element is a property of ownership xml`() {
        val elements =
            ownershipXsds()
                .flatMap { xsd ->
                    dom(xsd.readBytes()).getElementsByTagNameNS(XS, "element").let { nodes ->
                        List(nodes.length) { nodes.item(it) as Element }
                    }
                }.map { it.getAttribute("name") }
                .filter { it.isNotEmpty() && it != "ownershipDocument" }
                .toSortedSet()
        val properties = mutableSetOf<String>()
        collectProperties(strict.constructType(OwnershipDocument::class.java), properties, mutableSetOf())

        assertEquals(sortedSetOf(), elements - properties, "XSD elements without a property")
    }

    @Test
    fun `every XSD transaction code is known`() {
        val common = latestSpec().resolve("schemas/ownershipDocumentCommon.xsd.xml").readText()
        val picklist =
            Regex("""name="TRANS_CODE_PICKLIST".*?<xs:pattern value="([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
        val codes = assertNotNull(picklist.find(common), "TRANS_CODE_PICKLIST not found").groupValues[1].split('|')

        assertEquals(18, codes.size)
        assertEquals(
            emptyList(),
            codes.filter { TransactionCode.of(it) == null },
            "XSD codes missing from TransactionCode",
        )
    }

    private fun collectProperties(
        type: JavaType,
        names: MutableSet<String>,
        seen: MutableSet<JavaType>,
    ) {
        if (type.isContainerType) return collectProperties(type.contentType, names, seen)
        val raw = type.rawClass
        if (raw.isPrimitive || raw.name.startsWith("java.") || !seen.add(type)) return

        val description = strict._deserializationContext().introspectBeanDescriptionForCreation(type)
        description.findProperties().filter { it.couldDeserialize() }.forEach {
            names += it.name
            collectProperties(it.primaryType, names, seen)
        }
    }

    // Every footnote ID the model holds, found by walking its values.
    private fun footnoteRefs(node: Any?): Int =
        when (node) {
            null -> {
                0
            }

            is Footnoted<*> -> {
                node.footnoteIds.size
            }

            is Coding -> {
                node.footnoteIds.size
            }

            is Collection<*> -> {
                node.sumOf { footnoteRefs(it) }
            }

            else -> {
                if (node.javaClass.packageName != javaClass.packageName) {
                    0
                } else {
                    node.javaClass.declaredFields
                        .filterNot { Modifier.isStatic(it.modifiers) }
                        .sumOf { field -> field.trySetAccessible().let { footnoteRefs(field.get(node)) } }
                }
            }
        }

    private fun xmlBlock(accessionNumber: String): String {
        val text = Fixtures.bytes("ownership/$accessionNumber.txt").decodeToString()
        return text.substringAfter("<XML>").substringBefore("</XML>").trim()
    }

    private fun dom(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        return factory.newDocumentBuilder().parse(bytes.inputStream())
    }

    private fun latestSpec(): Path =
        Path.of("../specs").listDirectoryEntries("ownership-xml-v*").maxBy { dir ->
            dir.name.removePrefix("ownership-xml-v").split('.').map(String::toInt).let { (major, minor) ->
                major * 1000 +
                    minor
            }
        }

    private fun ownershipXsds(): List<Path> =
        latestSpec().resolve("schemas").listDirectoryEntries("ownership*").also {
            check(it.isNotEmpty()) { "No ownership XSDs in ${latestSpec()}" }
        }

    private companion object {
        const val XS = "http://www.w3.org/2001/XMLSchema"

        @JvmStatic
        fun fixtures(): List<String> =
            Files.list(Path.of("src/testFixtures/resources/edgar/ownership")).use { files ->
                files
                    .map { it.name }
                    .filter { it.endsWith(".txt") }
                    .map { it.removeSuffix(".txt") }
                    .sorted()
                    .toList()
            }
    }
}
