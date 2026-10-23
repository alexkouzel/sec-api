package com.alexkouzel.sec.drift

import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path

// Versions this library is known to handle, from specs/versions.json. Add a version there
// once its filings parse and the model covers it.
internal data class KnownVersions(
    val ownershipSpec: String,
    val schemaVersions: Set<String>,
) {
    companion object {
        fun load(file: Path): KnownVersions {
            val root = JsonMapper().readTree(file.toFile())
            val schemaVersions = root.path("schemaVersions")
            return KnownVersions(
                ownershipSpec = root.path("ownershipSpec").asString(),
                schemaVersions = List(schemaVersions.size()) { schemaVersions.get(it).asString() }.toSet(),
            )
        }
    }
}
