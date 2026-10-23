package com.alexkouzel.sec

private const val LOCAL_USER_AGENT = "TestCompany test.company@gmail.com"

// User-Agent from env EDGAR_USER_AGENT ("Name contact@domain"), with a fallback for local runs.
internal fun liveClient(): SecClient {
    val userAgent = System.getenv("EDGAR_USER_AGENT")
    if (userAgent.isNullOrBlank()) return SecClient(LOCAL_USER_AGENT)
    return SecClient(userAgent)
}
