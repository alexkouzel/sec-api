package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException
import com.alexkouzel.sec.UnknownFieldListener

internal interface Http {
    val unknownFields: UnknownFieldListener? get() = null

    fun get(url: String): ByteArray
}

// Parses a response, adding its URL to parse errors and to unknown fields.
internal fun <T> Http.get(
    url: String,
    parse: (ByteArray) -> T,
): T {
    val body = get(url)
    return try {
        collectingUnknownFields({ unknownFields?.onUnknownField(url, it) }) { parse(body) }
    } catch (e: SecException.Parse) {
        throw SecException.Parse("${e.message} ($url)", url, e)
    }
}
