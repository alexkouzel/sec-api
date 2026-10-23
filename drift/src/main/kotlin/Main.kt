package com.alexkouzel.sec.drift

import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.system.exitProcess

// Usage: drift <report dir> <specs dir>; reads the User-Agent from env EDGAR_USER_AGENT.
fun main(args: Array<String>) {
    val dir = Path.of(args.getOrElse(0) { "build/drift" })
    val specs = Path.of(args.getOrElse(1) { "../specs" })
    val userAgent =
        System.getenv("EDGAR_USER_AGENT")?.takeIf { it.isNotBlank() }
            ?: error("Set EDGAR_USER_AGENT to \"Name contact@domain\"")

    val report = Report(dir, LocalDate.now(ZoneOffset.UTC))
    try {
        DriftCheck(userAgent, KnownVersions.load(specs.resolve("versions.json")), report).run()
    } finally {
        report.write()
    }

    println("${report.findings.size} findings; report in ${dir.resolve("report.md")}")
    if (report.findings.isNotEmpty()) exitProcess(1)
}
