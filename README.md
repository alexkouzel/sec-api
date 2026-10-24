# SEC API

Kotlin library for [SEC EDGAR](https://www.sec.gov/edgar) data, usable from Java too.

- **Companies:** every company with a ticker: CIK, name, ticker, exchange.
- **Filing references:** from the latest filings feed, the daily list, a quarter's full
  index, or a company's (or insider's) whole filing history. Filtered by exact form type.
- **Ownership filings:** insider Forms 3, 4 and 5 (and amendments), parsed into issuer,
  reporting owners, transactions, holdings and footnotes.
- **Fair access built in:** at most 10 requests per second, retries with backoff after I/O
  errors, HTTP 429 (honouring `Retry-After`) and 5xx.
- **Watched against EDGAR:** fields EDGAR adds are ignored, and a daily check in this
  repository runs the library against live EDGAR and opens an issue when a change breaks it.

## Requirements

Java 21 or newer. Dependencies: Jackson 3 (`jackson-databind`) and the Kotlin standard library,
pulled in by Gradle or Maven.

## Installation

Releases are built by [JitPack](https://jitpack.io) from this repository's tags; no account or
token is needed. Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.alexkouzel:sec-api:v<version>")
}
```

The versions are the tags on the [releases page](https://github.com/alexkouzel/sec-api/releases).
Maven works too, with the repository `https://jitpack.io` and the same coordinates.

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
        val shares = transaction.shares?.value
        val price = transaction.pricePerShare?.value
        println("${transaction.date?.value} ${transaction.coding?.code}: $shares @ $price")
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
  footnoteIds)`; a derivative security's terms are in `derivative`. Share counts and prices
  are `BigDecimal`s with the digits EDGAR wrote, so compare them with `compareTo`.
- With only an accession number (from a link or a search), use
  `sec.ownership.get(cik, accessionNumber)`, where `cik` is the issuer's or a reporting owner's.

### Errors

Methods throw `SecException.Http` when a request still fails after 3 attempts and
`SecException.Parse` when a response isn't in the expected format (EDGAR changed it); both are
unchecked and name the URL in their message. Invalid arguments (a `count` the feed doesn't
offer, quarter 5) throw `IllegalArgumentException`.

## Development

See [CLAUDE.md](CLAUDE.md) for commands and [docs/](docs/) for the architecture and
conventions. In Claude Code, `/edgar-change` adapts the library when the daily live check fails
and `/release` releases the next version, worked out from the commits. In short:

```bash
./gradlew check       # offline: unit tests and ktlint
./gradlew liveTest    # against live EDGAR; set EDGAR_USER_AGENT
```
