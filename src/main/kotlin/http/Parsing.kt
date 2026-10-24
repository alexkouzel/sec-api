package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import org.w3c.dom.Element
import org.xml.sax.SAXException
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.xml.parsers.DocumentBuilderFactory

// Responses are read as trees (JSON nodes, XML elements), so fields EDGAR adds are simply
// never looked at.

private val json = JsonMapper()

internal fun parseJson(content: ByteArray): JsonNode {
    try {
        return json.readTree(content)
    } catch (e: JacksonException) {
        throw SecException.Parse("Invalid JSON: ${e.originalMessage}", e)
    }
}

internal fun parseXml(content: ByteArray): Element {
    val factory = DocumentBuilderFactory.newInstance()
    // EDGAR's XML has no DTD; refusing one also rules out external entities.
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    try {
        return factory.newDocumentBuilder().parse(content.inputStream()).documentElement
    } catch (e: SAXException) {
        throw SecException.Parse("Invalid XML: ${e.message}", e)
    }
}

// The child elements with this name, in document order; none for a missing parent.
internal fun Element?.children(name: String): List<Element> {
    if (this == null) return emptyList()
    return childElements().filter { it.tagName == name }
}

internal fun Element.hasChildElements(): Boolean = childElements().isNotEmpty()

private fun Element.childElements(): List<Element> {
    val nodes = childNodes
    return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>()
}

internal fun Element.child(name: String): Element? = children(name).firstOrNull()

// The text of a child element, trimmed; null when the element is missing or empty.
internal fun Element.text(name: String): String? = child(name)?.trimmedText()

internal fun Element.trimmedText(): String? = textContent.trim().takeIf { it.isNotEmpty() }

// An attribute's value; null when it is missing or empty.
internal fun Element.attribute(name: String): String? =
    getAttribute(name).trim().takeIf { it.isNotEmpty() }

// yyyy-MM-dd unless a format is given; anything else means the format changed.
internal fun parseDate(
    text: String,
    format: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE,
): LocalDate {
    try {
        return LocalDate.parse(text, format)
    } catch (e: DateTimeParseException) {
        throw SecException.Parse("Invalid date: $text", e)
    }
}
