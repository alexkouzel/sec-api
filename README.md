# SEC API

[![CI](https://github.com/alexkouzel/sec-api/actions/workflows/ci.yml/badge.svg)](https://github.com/alexkouzel/sec-api/actions/workflows/ci.yml)
[![Live EDGAR check](https://github.com/alexkouzel/sec-api/actions/workflows/live.yml/badge.svg)](https://github.com/alexkouzel/sec-api/actions/workflows/live.yml)
[![JitPack](https://jitpack.io/v/alexkouzel/sec-api.svg)](https://jitpack.io/#alexkouzel/sec-api)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A Kotlin library for [SEC EDGAR](https://www.sec.gov/edgar): companies, filing lists and insider
trades (Forms 3, 4 and 5) as typed objects. Works from Java too.

```kotlin
SecClient("Your Name you@example.com").use { sec ->
    val ref = sec.filings.company(320193).first { it.formType == FormType.F4 } // Apple
    val filing = sec.ownership.get(ref)
    val owner = filing.owners.first().name

    for (transaction in filing.transactions) {
        val shares = transaction.shares?.value
        println("$owner: ${transaction.coding?.code}, $shares shares")
    }
}
```

## Features

- **Companies:** every company with a ticker, with its CIK, name, ticker and exchange.
- **Filing lists:** the latest filings, a day's filings, a quarter's full index, or the whole
  history of a company or an insider, filtered by exact form type.
- **Insider trades:** Forms 3, 4 and 5 (and amendments), with issuer, reporting owners,
  transactions, holdings and footnotes. Numbers are exact `BigDecimal`s.
- **Fair access built in:** at most 10 requests per second, with retries and backoff after
  network errors, HTTP 429 (honouring `Retry-After`) and 5xx.
- **Checked daily against EDGAR:** a scheduled job runs the library against live EDGAR, so a
  format change is noticed within a day (the second badge above).

## Installation

Releases are built by [JitPack](https://jitpack.io/#alexkouzel/sec-api) from this repository's
tags; no account or token is needed. Requires Java 21 or newer.

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.alexkouzel:sec-api:v2.1.0")
}
```

The latest version is on the [releases page](https://github.com/alexkouzel/sec-api/releases).
Maven works too, with the repository `https://jitpack.io` and the same coordinates.

## Usage

EDGAR asks every client to identify itself as `Name contact@domain`. Create one `SecClient` and
share it: it is safe to use from several threads and keeps all of them under EDGAR's limit of
10 requests per second. Calls block; close the client when done.

### Companies

```kotlin
val companies = sec.companies.all() // Company(cik, name, ticker, exchange)
```

A company with several tickers appears once per ticker. `exchange` is null where EDGAR lists
none.

### Filing lists

Each call returns a list of `FilingRef(accessionNumber, cik, formType, filedOn)`, each filing
once.

| Call                              | Lists                                | Notes                                        |
|-----------------------------------|--------------------------------------|----------------------------------------------|
| `filings.latest(count, formType)` | the newest filings                   | `count` is 10, 20, 40 (default), 80 or 100   |
| `filings.day(daysAgo, formType)`  | one day's filings                    | `0` is today; empty on weekends and holidays |
| `filings.quarter(year, quarter)`  | a calendar quarter's full index      | since 1993; tens of megabytes                |
| `filings.company(cik)`            | every filing of a company or insider | one request per ~1,000 filings               |

```kotlin
val form4s = sec.filings.day(daysAgo = 1, formType = FormType.F4)
val q3 = sec.filings.quarter(2026, 3)
```

`FormType` keeps EDGAR's raw value (`FormType("424B2")`), with constants for common forms:
`F3`, `F4`, `F5`, their amendments `F4_A`, ..., and `F8_K`, `F10_K`, `F10_Q`. The `formType` filter
matches exactly, so `latest` may return fewer filings than `count`, or none.

`FilingRef.cik` is a CIK related to the filing; its role depends on the list (the issuer's on
the latest feed where one is named, the requested one for `company`).

### Insider trades (Forms 3, 4, 5)

```kotlin
val filing = sec.ownership.get(ref) // from a FilingRef
val sameFiling = sec.ownership.get(1000753, "0001127602-23-028345") // CIK + accession number

filing.issuer // Issuer(cik, name, ticker, ...)
filing.owners // reporting owners, with address and relationship
filing.transactions // what was bought, sold, granted or exercised
filing.holdings // what is held without a transaction
filing.footnotes // the texts footnote ids refer to
```

- Values that can carry footnotes are `Footnoted<T>(value, footnoteIds)`; read them with
  `.value`.
- `derivative` is set on transactions and holdings of options, warrants and other derivative
  securities, with their exercise price, dates and underlying security.
- Share counts and prices are `BigDecimal`s with the digits EDGAR wrote (`24.4000`), so compare
  them with `compareTo`, not `==`.
- `coding?.code` is a `TransactionCode` (`PURCHASE`, `SALE`, `OPTION_EXERCISE`, ...);
  `coding.value` keeps EDGAR's letter.

### From Java

```java
try (SecClient sec = new SecClient("Your Name you@example.com")) {
    List<FilingRef> refs = sec.getFilings().latest(40, FormType.F4);
    OwnershipFiling filing = sec.getOwnership().get(refs.get(0));
}
```

### Errors

| Exception                  | Thrown when                                                                      |
|----------------------------|----------------------------------------------------------------------------------|
| `SecException.Http`        | a request fails: after 3 attempts for I/O errors, 429 and 5xx, at once otherwise |
| `SecException.Parse`       | a response isn't in the expected format: EDGAR changed it                        |
| `IllegalArgumentException` | an argument is invalid, such as quarter 5                                        |

Both `SecException`s are unchecked and name the URL in their message.

## Development

```bash
./gradlew check       # offline: unit tests and ktlint
./gradlew liveTest    # against live EDGAR; set EDGAR_USER_AGENT
```

[CLAUDE.md](CLAUDE.md) has the commands and how the project is managed; [docs/](docs/) has the
architecture and conventions. In Claude Code, `/fix-live-check` adapts the library when the
daily check fails, and `/improve` opens a pull request with improvements to the code and docs.

## License

[MIT](LICENSE)
