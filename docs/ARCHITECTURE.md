# Architecture

How the library and its tools are laid out, and why. Code rules are in
[CONVENTIONS.md](CONVENTIONS.md).

## Principles

1. **Organize by area, not by kind.** A package is an EDGAR concept (`filing`, `ownership`),
   never a role (`utils`, `exceptions`, `deserializers`, `common`). Code lives next to its
   only user. A helper used in two areas goes into the lower layer; if there isn't one,
   the helper is probably wrong.
2. **Small public surface.** Explicit API mode. Public: the entry point, the area facades,
   the models and the error type. Everything else is `internal`.
3. **EDGAR's formats stay at the edge.** Each source's wire shape (JSON columns, Atom,
   `<pre>` HTML, `master.idx`, ownership XML) is parsed by one internal file in its area and
   mapped to the model. The model is shaped for users, not for EDGAR.
4. **One way in.** A single `SecClient`; no loaders built by hand, no subclassing.
5. **Immutable values.** Models are data classes of `val`s with read-only collections.
6. **Tolerant of additions, loud about changes.** Unknown fields are ignored (and reported
   to an optional listener); an unrecognized layout fails with `SecException.Parse`.
7. **Dependencies point one way:** `ownership → filing → http`, `company → http`;
   `SecClient` wires them together. No cycles, no area reaching into another's internals.

## Modules

```
sec-api/
├── lib/          the published library
├── drift/        drift-check tool; uses only lib's public API
├── build-logic/  convention plugin sec.kotlin: toolchain, ktlint, detekt, JUnit
├── config/       detekt.yml (differences from detekt's defaults only)
├── specs/        SEC specifications and specs/versions.json
└── docs/
```

`drift` is an application, not a test: it samples live EDGAR and writes a report
(`Main`, `DriftCheck`, `Report`, `SpecPage`, `KnownVersions` in `drift/src/main/kotlin`). Being a
separate module keeps the library free of tooling and makes it a real consumer of the API
(it sees drift through the same unknown-field listener library users get).

## Library packages

Root package `com.alexkouzel.sec`. Following Kotlin's convention, directories omit the
root: `lib/src/main/kotlin/filing/FilingRef.kt` is `com.alexkouzel.sec.filing`.

```
lib/src/main/kotlin/
├── SecClient.kt          entry point: SecClient(userAgent, ...) with .companies, .filings, .ownership
├── UnknownFieldListener.kt  told about fields EDGAR added (used by drift)
├── SecException.kt       sealed: Http, Parse
├── http/                 transport only, knows nothing about EDGAR formats
│   ├── Http.kt           internal interface: get(url) -> bytes; the seam tests replace
│   ├── Mapping.kt        internal: JSON and XML mappers shared by the areas, unknown fields ignored
│   ├── EdgarHttp.kt      JDK HttpClient, User-Agent, gzip/deflate, retries
│   └── RateLimiter.kt
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
    ├── Ownership.kt      facade: get(ref)
    ├── OwnershipFiling.kt
    ├── Owner.kt          Issuer, ReportingOwner, Relationship, Signature
    ├── Transaction.kt    Transaction, Holding (derivative and not), Coding, TransactionCode
    ├── Footnote.kt       Footnote, Footnoted<T>
    ├── FilingText.kt     internal: SGML header and <XML> block
    └── xml/              internal: classes mirroring the XSD, mapped to the model
```

Notes:

- A package gets a subpackage only for a separate concept with its own vocabulary;
  `ownership/xml` qualifies (XSD names, Jackson annotations). Otherwise packages stay flat.
- Small types that belong together share a file named after the concept (`Owner.kt`),
  instead of one file per 10-line class.
- `ownership/xml` exists because the XML is deep and awkward
  (`transactionAmounts.transactionShares.value` plus `footnoteId`s). The model flattens it,
  e.g. `transaction.shares: Footnoted<Double>?`. Contract tests compare the XSD with
  `ownership/xml`, so new elements are still caught.
- `FormType` keeps the raw value (`"424B2"`) with constants for the common forms, instead
  of an enum that collapses everything else into `OTHER`. `TransactionCode` is an enum, but
  `Coding.value` keeps the raw code, so an unknown one isn't lost.
- Transactions and holdings of both tables share `Transaction` and `Holding`; a derivative
  security's terms (exercise price, dates, underlying security) are in `derivative`, null
  for the security itself.
- `FilingRef.cik` is a related CIK, not an issuer's: its role depends on the source (see the
  glossary). EDGAR serves a filing under each related CIK.

## Unknown fields and drift

Wire classes (`ownership/xml`, the submissions and latest feed classes, `Tickers`) declare
every field EDGAR sends, read or not (unread ones as `Any?`). A Jackson problem handler on the
shared mappers in `http/Mapping.kt` reports anything else to the `UnknownFieldListener` of
the request being parsed, and parsing carries on. One mechanism serves three users:

- library users, through `SecClient(..., unknownFields)`;
- the contract tests, which collect unknown fields while parsing every fixture and expect none;
- the `drift` tool, which samples live EDGAR weekly and reports them.

So the runtime is tolerant of additions and the tests are strict about them. A changed
layout (a missing field, an unrecognized row) fails with `SecException.Parse` instead.

Known Jackson behaviour to keep in mind:

- jackson-module-kotlin applies a property's non-null element type to nested collections
  too, so `List<List<Any?>>` rejects a `null` inside an inner list; `Tickers` reads each row
  as a `JsonNode`.
- With the problem handler installed, `FAIL_ON_UNKNOWN_PROPERTIES` never fires, so tests
  check unknown fields through the listener mechanism, not a strict mapper.
- Repeated XML elements may be interleaved with others (valid per XSD); `@field:JsonMerge`
  on each list keeps every group.

## Public API sketch

```kotlin
SecClient(userAgent = "Name contact@domain").use { sec ->
    val companies = sec.companies.all()
    val refs = sec.filings.latest(count = 40, form = FormType.F4)
    val filing = sec.ownership.get(refs.first())
}
```

Calls are blocking, so Java callers use the same API (`sec.getFilings().latest(40, FormType.F4)`).

## Tests

Each kind of test is its own source set, so a folder's name says what is in it:

```
lib/src/
├── main/
├── test/            unit tests only; mirrors main: filing/LatestFeed.kt -> filing/LatestFeedTest.kt
├── testFixtures/    shared support: Fixtures, FixtureHttp, LocalServer; resources/edgar/ (frozen fixtures)
├── contractTest/    no unknown fields in any fixture, XSD vs ownership/xml, transaction codes
└── liveTest/        smoke tests against real EDGAR
drift/src/
├── main/            the drift-check tool
└── test/            its unit tests (report, spec page, known versions)
```

| Gradle task | Runs | Network |
|-------------|------|---------|
| `test` | `test` | no |
| `contractTest` | `contractTest` | no |
| `check` | `test`, `contractTest`, ktlint, detekt | no |
| `liveTest` | `liveTest` | EDGAR |
| `:drift:driftCheck` | `drift` app | EDGAR, SEC spec page |
| `knownBugTest` | tests tagged `known-bug` | no |

Rules:

- In `test`, every file is `<Name>Test.kt` next to the package of the `<Name>.kt` it tests.
  No helpers, no test-only clients.
- Support code is written once, in `testFixtures`. `testFixtures`, `contractTest` and
  `liveTest` are associated with `main`, so they can use `internal` code (e.g. `Http`);
  `test`, `contractTest` and `liveTest` are associated with `testFixtures` too, so its
  support code can stay `internal` (e.g. `FixtureHttp`).
- The `known-bug` tag stays for tests that document an open bug.

## Build

- Gradle Kotlin DSL, versions in `gradle/libs.versions.toml`, settings shared by a
  convention plugin in `build-logic/` (toolchain, ktlint, detekt, test tasks).
- Jackson 3 (`tools.jackson`, with `jackson-module-kotlin`): it handles the XML well, its
  exceptions are unchecked, and java.time support is built in.
