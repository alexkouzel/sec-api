package com.alexkouzel.sec

// EDGAR responses under src/test/resources/edgar, frozen (see its README).
internal object Fixtures {
    fun bytes(path: String): ByteArray {
        val stream = Fixtures::class.java.getResourceAsStream("/edgar/$path")
        requireNotNull(stream) { "No fixture: edgar/$path" }
        return stream.use { it.readBytes() }
    }
}
