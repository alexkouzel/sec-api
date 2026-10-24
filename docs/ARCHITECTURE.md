# Architecture

How the library is laid out, and why. Code rules are in [CONVENTIONS.md](CONVENTIONS.md).

## Principles

1. **Organize by area, not by kind.** A package is an EDGAR concept (`filing`, `ownership`),
   never a role (`utils`, `exceptions`, `common`). Code lives next to its only user; a helper
   used by two areas goes into the lower layer (`http`).
2. **Small public surface.** Explicit API mode. Public: `SecClient`, the area facades, the
   models and `SecException`. Everything else is `internal`.
3. **EDGAR's formats stay at the edge.** Each source's wire shape (JSON columns, Atom, `<pre>`
   HTML, `master.idx`, ownership XML) is parsed by one internal file in its area and mapped to
   a model shaped for users.
4. **One way in.** A single `SecClient`; no loaders built by hand.
5. **Immutable values.** Models are data classes of `val`s with read-only collections.
6. **Tolerant of additions, loud about changes.** Fields EDGAR adds are ignored. A changed
   layout (a missing value, an unrecognized row) throws `SecException.Parse` naming the URL.
7. **Dependencies point one way:** `ownership → filing → http`, `company → http`;
   `SecClient` wires them together.

## Packages

Root package `com.alexkouzel.sec`. Directories omit the root, as Kotlin recommends:
`src/main/kotlin/filing/FilingRef.kt` is `com.alexkouzel.sec.filing`.

```
src/main/kotlin/
├── SecClient.kt          entry point: SecClient(userAgent) with .companies, .filings, .ownership
├── SecException.kt       sealed: Http, Parse
├── http/                 transport, knows nothing about EDGAR formats
│   ├── Http.kt           internal interface: get(url) -> bytes; the seam tests replace
│   ├── EdgarHttp.kt      JDK HttpClient, User-Agent, gzip/deflate, retries
│   ├── RateLimiter.kt    at most 10 requests per second
│   └── Parsing.kt        JSON and XML trees and helpers the areas share
├── company/
│   ├── Companies.kt      facade: all()
│   ├── Company.kt
│   └── Tickers.kt        internal: company_tickers_exchange.json
├── filing/
│   ├── Filings.kt        facade: latest(), daily(), quarter(), ofCompany()
│   ├── FilingRef.kt
│   ├── FormType.kt
│   ├── LatestFeed.kt     internal: Atom feed
│   ├── DailyList.kt      internal: cgi-bin/current
│   ├── QuarterlyIndex.kt internal: master.idx
│   └── Submissions.kt    internal: submissions JSON and its older files
└── ownership/
    ├── Ownership.kt      facade: get(ref), get(cik, accessionNumber)
    ├── OwnershipFiling.kt
    ├── Owner.kt          Issuer, ReportingOwner, Address, Relationship, Signature
    ├── Transaction.kt    Transaction, Holding, Derivative, Coding, TransactionCode
    ├── Footnote.kt       Footnote, Footnoted<T>
    ├── FilingText.kt     internal: SGML header and <XML> block
    ├── OwnershipXml.kt   internal: the ownership XML, element by element, into the model
    └── XmlValues.kt      internal: its values (footnoted values, numbers, dates, flags)
```

Notes:

- Responses are read as trees (Jackson `JsonNode`, the JDK's DOM), never mapped onto classes,
  so fields EDGAR adds are simply not looked at.
- The ownership XML is deep and awkward (`transactionAmounts.transactionShares.value` plus
  `footnoteId`s). `OwnershipXml.kt` reads it by the XSD's element names and flattens it into the
  model, e.g. `transaction.shares: Footnoted<BigDecimal>?`; `XmlValues.kt` turns its text
  into numbers, dates and flags and fails on a malformed one.
- Transactions and holdings of both tables share `Transaction` and `Holding`; a derivative
  security's terms (exercise price, dates, underlying security) are in `derivative`, null for
  the security itself.
- `FormType` keeps the raw value (`"424B2"`) with constants for common forms.
  `TransactionCode` is an enum, but `Coding.value` keeps the raw code, so an unknown one
  isn't lost.
- `FilingRef.cik` is a related CIK, not necessarily the issuer's; its role depends on the source
  (see the glossary). EDGAR serves a filing under each related CIK.
- Small types that belong together share a file named after the concept (`Owner.kt`).

## Public API

```kotlin
SecClient("Name contact@domain").use { sec ->
    val companies = sec.companies.all()
    val refs = sec.filings.latest(count = 40, form = FormType.F4)
    val filing = sec.ownership.get(refs.first())
}
```

Calls block, so Java uses the same API: `sec.getFilings().latest(40, FormType.F4)`.

## Tests

```
src/
├── main/
├── test/            unit tests, offline; mirrors main: filing/LatestFeed.kt -> filing/LatestFeedTest.kt
│                    helpers at its root: Fixtures, FixtureHttp, LocalServer; resources/edgar/ (frozen);
│                    java/: JavaApiTest, the API as Java sees it
└── liveTest/        the library against live EDGAR
```

| Gradle task | Runs | Network |
|-------------|------|---------|
| `check` | unit tests, ktlint, detekt | no |
| `liveTest` | `src/liveTest` | EDGAR |

- `test` and `liveTest` may use `internal` code of `main`.
- `ownership/OwnershipXmlTest` checks that the parser reads every element of the newest XSD in
  `specs/` and knows every transaction code, and that the model keeps every repeated element
  of each fixture.

## How EDGAR changes are caught

`.github/workflows/live.yml` runs `liveTest` every day. The live tests call every source and
check the values users rely on (issuer, owners, transaction dates and codes) in a sample of
recent Form 3, 4 and 5 filings. When EDGAR changes something the library reads, a test fails
with the URL of the response, and the workflow opens an issue labelled `edgar-change`.
Fields EDGAR only adds don't break anything and aren't reported.

## Build

- One Gradle project; plugin and library versions in `gradle/libs.versions.toml` (Dependabot
  proposes updates); detekt's differences from its defaults in `config/detekt.yml`.
- Releases are git tags; JitPack builds them (`jitpack.yml`) as `com.github.alexkouzel:sec-api`.
- One runtime dependency besides the Kotlin standard library: Jackson 3's `jackson-databind`
  (`tools.jackson`), for JSON trees. XML uses the JDK's DOM parser, with DTDs refused.
