package com.alexkouzel.sec.http

import com.alexkouzel.sec.SecException

internal fun interface Http {
    fun get(url: String): ByteArray
}

internal fun <T> Http.get(
    url: String,
    parse: (ByteArray) -> T,
): T {
    val body = get(url)
    return try {
        parse(body)
    } catch (e: SecException.Parse) {
        throw SecException.Parse("${e.message} ($url)", e)
    }
}
