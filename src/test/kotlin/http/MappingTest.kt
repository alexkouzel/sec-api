package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MappingTest {
    data class Item(
        val name: String,
    )

    @Test
    fun `maps JSON and ignores unknown fields`() {
        val item = json.map<Item>("""{"name": "x", "addedLater": true}""".toByteArray())

        assertEquals(Item("x"), item)
    }

    @Test
    fun `maps XML and ignores unknown elements`() {
        val item =
            xml.map<Item>(
                "<item><name>x</name><addedLater>1</addedLater></item>".toByteArray(),
            )

        assertEquals(Item("x"), item)
    }

    @Test
    fun `reports invalid content as a parse error`() {
        assertFailsWith<SecException.Parse> { json.map<Item>("not json".toByteArray()) }
    }
}
