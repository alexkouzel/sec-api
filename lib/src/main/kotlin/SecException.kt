package com.alexkouzel.sec

/** [url] is the request that failed, or whose response didn't parse. */
public sealed class SecException(
    message: String,
    public val url: String?,
    cause: Throwable?,
) : RuntimeException(message, cause) {
    public class Http internal constructor(
        message: String,
        url: String? = null,
        cause: Throwable? = null,
    ) : SecException(message, url, cause)

    public class Parse internal constructor(
        message: String,
        url: String? = null,
        cause: Throwable? = null,
    ) : SecException(message, url, cause)
}
