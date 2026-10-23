package com.alexkouzel.sec

// User-Agent from env EDGAR_USER_AGENT ("Name contact@domain"), with a fallback for local runs.
internal fun liveClient(): SecClient =
    SecClient(System.getenv("EDGAR_USER_AGENT")?.takeIf { it.isNotBlank() } ?: "TestCompany test.company@gmail.com")
