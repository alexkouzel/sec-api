package com.alexkouzel.sec.drift

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnownVersionsTest {
    private val known = KnownVersions.load(Path.of("../specs/versions.json"))

    @Test
    fun `reads specs versions json`() {
        assertContains(known.schemaVersions, "X0609")
    }

    @Test
    fun `the known spec is in specs and on the fixture page`() {
        val page = Path.of("../lib/src/testFixtures/resources/edgar/spec-page/technical-specifications-ownership.html")

        assertTrue(Files.isDirectory(Path.of("../specs/ownership-xml-v${known.ownershipSpec}")))
        assertEquals(
            known.ownershipSpec,
            SpecPage.currentVersion(page.readText()),
            "fixture page and versions.json disagree",
        )
    }
}
