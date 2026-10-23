package com.alexkouzel.drift;

import com.alexkouzel.testing.Fixtures;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecWatcherTest {
    private static final String PAGE = Fixtures.text("spec-page/technical-specifications-ownership.html");

    @Test
    public void readsCurrentVersionFromOwnershipSection() {
        assertEquals(Optional.of("5.5"), SpecWatcher.currentVersion(PAGE));
    }

    @Test
    public void readsLinkedPackages() {
        assertEquals(Set.of("5.4", "5.5"), SpecWatcher.linkedVersions(PAGE));
    }

    @Test
    public void marksDraftPackages() {
        String page = PAGE + "<a href=\"/files/ownershipxmltechspec-v5-6_d.zip\">Draft</a>";

        assertTrue(SpecWatcher.linkedVersions(page).contains("5.6 (draft)"));
    }

    @Test
    public void missingSectionIsEmpty() {
        assertEquals(Optional.empty(), SpecWatcher.currentVersion(PAGE.replace("id=\"ownership_forms\"", "")));
    }

    @Test
    public void comparesVersionsNumerically() {
        assertTrue(SpecWatcher.compareVersions("5.10", "5.9") > 0);
        assertTrue(SpecWatcher.compareVersions("5.6 (draft)", "5.5") > 0);
        assertEquals(0, SpecWatcher.compareVersions("5.5", "5.5.0"));
    }

    @Test
    public void knownSpecIsInSpecs() throws Exception {
        String spec = KnownVersions.load().ownershipSpec();

        assertTrue(Files.isDirectory(Path.of("specs/ownership-xml-v" + spec)), "specs/ownership-xml-v" + spec);
        assertEquals(Optional.of(spec), SpecWatcher.currentVersion(PAGE), "fixture page and specs/versions.json disagree");
    }
}
