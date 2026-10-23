package com.alexkouzel.sec

public sealed class SecException(
    message: String,
    cause: Throwable? = null,
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
