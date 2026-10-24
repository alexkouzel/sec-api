package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ParsingTest {
    @Test
    fun `reads JSON as a tree`() {
        val root = parseJson("""{"name": "x", "addedLater": true}""".toByteArray())

        assertEquals("x", root.path("name").asString())
    }

    @Test
    fun `reports invalid JSON as a parse error`() {
        assertFailsWith<SecException.Parse> { parseJson("not json".toByteArray()) }
    }

    @Test
    fun `keeps interleaved child elements in document order`() {
        val root = parseXml("<table><h>1</h><t>x</t><h>2</h></table>".toByteArray())

        assertEquals(listOf("1", "2"), root.children("h").map { it.textContent })
    }

    @Test
    fun `reads an empty element as null`() {
        val root = parseXml("<item><name> x </name><title></title></item>".toByteArray())

        assertEquals("x", root.text("name"))
        assertNull(root.text("title"))
        assertNull(root.text("missing"))
    }

    @Test
    fun `refuses XML with a DTD`() {
        val xml = """<?xml version="1.0"?><!DOCTYPE item [<!ENTITY x "y">]><item>&x;</item>"""

        assertFailsWith<SecException.Parse> { parseXml(xml.toByteArray()) }
    }

    @Test
    fun `reads ISO dates and rejects others`() {
        assertEquals(LocalDate.parse("2026-10-21"), isoDate("2026-10-21"))
        assertFailsWith<SecException.Parse> { isoDate("10-21-2026") }
    }
}
