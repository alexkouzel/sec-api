package com.alexkouzel.drift;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Spec and schema versions this library is known to handle, from specs/versions.json.
 * Add a version there once its filings parse and the model covers it.
 */
public record KnownVersions(String ownershipSpec, Set<String> schemaVersions) {
    public static final Path PATH = Path.of("specs/versions.json");

    public static KnownVersions load() throws IOException {
        return new ObjectMapper().readValue(PATH.toFile(), KnownVersions.class);
    }
}
