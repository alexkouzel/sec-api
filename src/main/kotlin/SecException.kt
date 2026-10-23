package com.alexkouzel.sec

/** A request to EDGAR failed ([Http]) or its response wasn't in the expected format ([Parse]). */
public sealed class SecException(
    message: String,
    cause: Throwable?,
) : RuntimeException(message, cause) {
    public class Http internal constructor(
        message: String,
        cause: Throwable? = null,
    ) : SecException(message, cause)

    public class Parse internal constructor(
        message: String,
        cause: Throwable? = null,
    ) : SecException(message, cause)
}
