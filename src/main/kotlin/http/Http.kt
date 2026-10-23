package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException

internal fun interface Http {
    fun get(url: String): ByteArray
}

// Fetches and parses a response; a parse error gets the URL added to its message.
internal fun <T> Http.get(
    url: String,
    parse: (ByteArray) -> T,
): T {
    val body = get(url)
    try {
        return parse(body)
    } catch (e: SecException.Parse) {
        throw SecException.Parse("${e.message} ($url)", e)
    }
}
