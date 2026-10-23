package com.alexkouzel.sec

import com.alexkouzel.sec.company.Companies
import com.alexkouzel.sec.filing.Filings
import com.alexkouzel.sec.http.EdgarHttp
import com.alexkouzel.sec.http.Http
import com.alexkouzel.sec.ownership.Ownership
import java.time.Duration

/**
 * The way into EDGAR. [userAgent] identifies you, as EDGAR's fair access policy requires:
 * `"Name contact@domain"`. Calls block; requests are kept under 10 per second per client and
 * retried after I/O errors, HTTP 429 and 5xx, waiting [baseRetryDelay], then twice that, and
 * so on up to [maxRetryDelay]. [unknownFields] hears about fields EDGAR added that this library
 * doesn't read yet. Close the client when done.
 */
public class SecClient internal constructor(
    private val http: Http,
) : AutoCloseable {
    @JvmOverloads
    public constructor(
        userAgent: String,
        baseRetryDelay: Duration = Duration.ofSeconds(1),
        maxRetryDelay: Duration = Duration.ofMinutes(1),
        unknownFields: UnknownFieldListener? = null,
    ) : this(EdgarHttp(validUserAgent(userAgent), baseRetryDelay, maxRetryDelay, unknownFields))

    public val companies: Companies = Companies(http)
    public val filings: Filings = Filings(http)
    public val ownership: Ownership = Ownership(http)

    override fun close() {
        (http as? AutoCloseable)?.close()
    }

    private companion object {
        fun validUserAgent(userAgent: String): String {
            require('@' in userAgent) { "userAgent must be \"Name contact@domain\": $userAgent" }
            return userAgent
        }
    }
}
