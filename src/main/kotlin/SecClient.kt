package com.alexkouzel.sec

import com.alexkouzel.sec.company.Companies
import com.alexkouzel.sec.filing.Filings
import com.alexkouzel.sec.http.EdgarHttp
import com.alexkouzel.sec.http.Http
import com.alexkouzel.sec.ownership.Ownership

/**
 * The way into EDGAR. [userAgent] identifies you, as EDGAR's fair access policy requires:
 * `"Name contact@domain"`. Calls block. Requests stay under 10 per second and are retried after
 * I/O errors, HTTP 429 and 5xx. Close the client when done.
 */
public class SecClient internal constructor(
    private val http: Http,
) : AutoCloseable {
    public constructor(userAgent: String) : this(EdgarHttp(validUserAgent(userAgent)))

    public val companies: Companies = Companies(http)
    public val filings: Filings = Filings(http)
    public val ownership: Ownership = Ownership(http)

    override fun close() {
        if (http is AutoCloseable) {
            http.close()
        }
    }

    private companion object {
        fun validUserAgent(userAgent: String): String {
            require('@' in userAgent) { "userAgent must be \"Name contact@domain\": $userAgent" }
            return userAgent
        }
    }
}
