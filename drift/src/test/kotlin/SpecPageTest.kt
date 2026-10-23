package com.alexkouzel.sec.drift

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpecPageTest {
    private val page =
        Path.of("../lib/src/testFixtures/resources/edgar/spec-page/technical-specifications-ownership.html").readText()

    @Test
    fun `reads the current version from the Ownership section`() {
        assertEquals("5.5", SpecPage.currentVersion(page))
    }

    @Test
    fun `reads the linked packages`() {
        assertEquals(setOf("5.4", "5.5"), SpecPage.linkedVersions(page))
    }

    @Test
    fun `marks draft packages`() {
        val draft = page + "<a href=\"/files/ownershipxmltechspec-v5-6_d.zip\">Draft</a>"

        assertContains(SpecPage.linkedVersions(draft), "5.6 (draft)")
    }

    @Test
    fun `a page without the section has no version`() {
        assertNull(SpecPage.currentVersion(page.replace("id=\"ownership_forms\"", "")))
    }

    @Test
    fun `compares versions numerically`() {
        assertTrue(SpecPage.compareVersions("5.10", "5.9") > 0)
        assertTrue(SpecPage.compareVersions("5.6 (draft)", "5.5") > 0)
        assertEquals(0, SpecPage.compareVersions("5.5", "5.5.0"))
    }
}
