# SEC API

Kotlin library for [SEC EDGAR](https://www.sec.gov/edgar) data, usable from Java too.

- **Companies:** every company with a ticker: CIK, name, ticker, exchange.
- **Filing references:** from the latest filings feed, the daily list, a quarter's full
  index, or a company's (or insider's) whole filing history. Filtered by exact form type.
- **Ownership filings:** insider Forms 3, 4 and 5 (and amendments), parsed into issuer,
  reporting owners, transactions, holdings and footnotes.
- **Fair access built in:** at most 10 requests per second, retries with backoff after I/O
  errors, HTTP 429 (honouring `Retry-After`) and 5xx.
- **Tolerant of EDGAR changes:** fields EDGAR adds are skipped and can be reported to a
  listener; a weekly drift check in this repository watches for them.

## Requirements

Java 21 or newer. Dependencies: Jackson 3 (`tools.jackson`) and the Kotlin standard library,
pulled in by Gradle or Maven.

## Installation

Published to GitHub Packages as `com.alexkouzel:sec-api` (reading from GitHub Packages needs
a GitHub token with `read:packages`):

```kotlin
repositories {
    maven("https://maven.pkg.github.com/alexkouzel/sec-api") {
        credentials {
            username = providers.gradleProperty("gpr.username").get()
            password = providers.gradleProperty("gpr.token").get()
        }
    }
}

dependencies {
    implementation("com.alexkouzel:sec-api:<version>")
}
```

Or build it: `./gradlew :lib:publishToMavenLocal`, then depend on it from `mavenLocal()`.

## Usage

EDGAR asks every client to identify itself as `Name contact@domain`. Create one `SecClient`,
share it, and close it when done; calls block.

```kotlin
SecClient("Sample Company admin@example.com").use { sec ->
    val companies = sec.companies.all()

    val latest = sec.filings.latest(count = 100, form = FormType.F4)
    val threeDaysAgo = sec.filings.daily(daysAgo = 3, form = FormType.F4)
    val quarter = sec.filings.quarter(2023, 3)
    val tesla = sec.filings.ofCompany(1318605)

    val filing = sec.ownership.get(latest.first())
    println("${filing.issuer.name} (${filing.issuer.ticker}), filed ${filing.filedOn}")
    for (transaction in filing.transactions) {
        println("${transaction.date?.value} ${transaction.coding?.code} ${transaction.shares?.value} @ ${transaction.pricePerShare?.value}")
    }
}
```

From Java:

```java
try (SecClient sec = new SecClient("Sample Company admin@example.com")) {
    List<FilingRef> refs = sec.getFilings().latest(40, FormType.F4);
    OwnershipFiling filing = sec.getOwnership().get(refs.get(0));
}
```

### What you get

- `FilingRef(accessionNumber, cik, formType, filedOn)`. `cik` is a CIK related to the filing;
  its role depends on the source (the issuer's on the latest feed where one is named, the
  requested one for `ofCompany`).
- `FormType` keeps EDGAR's value (`FormType("424B2")`), with constants for common forms
  (`F3`, `F4`, `F5`, their amendments `F4_A`, ..., `F8_K`, `F10_K`, `F10_Q`). A form filter
  matches exactly, so `latest` may return fewer filings than `count`, or none.
- `OwnershipFiling`: header values, `issuer`, `owners`, `transactions`, `holdings`,
  `footnotes`, `signatures`. Values that can carry footnotes are `Footnoted<T>(value,
  footnoteIds)`; a derivative security's terms are in `derivative`.

### Errors

Methods throw `SecException.Http` when a request still fails after 3 attempts and
`SecException.Parse` when a response isn't in the expected format; both are unchecked and
carry the `url`. Invalid arguments (a `count` the feed doesn't offer, quarter 5) throw
`IllegalArgumentException`.

### Fields EDGAR adds

EDGAR adds fields from time to time. They are skipped, so parsing keeps working; to hear about
them, pass a listener:

```kotlin
SecClient(userAgent, unknownFields = { url, field -> log.warn("EDGAR added $field ($url)") })
```

## Development

See [CLAUDE.md](CLAUDE.md) for commands and the drift runbook, and [docs/](docs/) for the
architecture and conventions. In short:

```bash
./gradlew check                # offline: unit and contract tests, ktlint, detekt
./gradlew liveTest             # against real EDGAR; set EDGAR_USER_AGENT
./gradlew :drift:driftCheck    # compares live EDGAR with the library
```
