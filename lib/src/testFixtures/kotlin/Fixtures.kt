package com.alexkouzel.sec

// EDGAR responses under resources/edgar, frozen (see its README).
internal object Fixtures {
    fun bytes(path: String): ByteArray =
        Fixtures::class.java.getResourceAsStream("/edgar/$path")?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No fixture: edgar/$path")
}
