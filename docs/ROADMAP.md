# Roadmap

Plan for bringing sec-api back to a working state and rewriting it in Kotlin.
Update this file in the same commit that completes (or changes) a phase.

## Status

| Phase | Goal | Status |
|-------|------|--------|
| 0 | Build compiles on a current JDK | Done |
| 1 | Verify against live EDGAR, refresh specs | Done |
| 2 | Automated tests (offline fixtures, mock client, opt-in live) | Done |
| 3 | Fix known bugs in Java | Done |
| 4 | Spec drift monitoring (CI, scheduled live check) | Done |
| 5 | Kotlin rewrite and redesign | In progress |
| 6 | Final `CLAUDE.md` and README | Not started |

## Phases

### Phase 0 — Build

- Pin a JDK toolchain (21) in Gradle; upgrade the Gradle wrapper and the Lombok plugin.
- No logic changes. Done when `./gradlew compileJava compileTestJava` succeeds.
- Result: Gradle 8.5 → 9.8.2, Lombok plugin 8.4 → 9.8.2, Java 21 toolchain auto-provisioned
  via the foojay resolver, JUnit platform launcher added (required by Gradle 9).
  Before: JDK 25 crashed in Lombok during `compileJava`; JDK 17 failed on `getFirst()` in tests.

### Phase 1 — Verify against live EDGAR

- Exercise every data source: company tickers, submissions JSON, latest Atom feed,
  daily `cgi-bin/current` page, quarterly `master.idx`, Form 3/4/5 `.txt` filings.
- Save raw responses as fixtures under `src/test/resources`.
- Record each break in the findings log below: EDGAR changed, code bug, or outdated spec.
- Replace `specs/` with dated copies of the current SEC documents and a `specs/README.md`
  listing each source and its date.
- Fix only what blocks basic operation; everything else goes to Phase 3.

### Phase 2 — Tests

- Offline parser tests on fixtures with exact expected values (amendments, footnotes,
  multiple owners, derivative and non-derivative tables).
- Client tests against a mock HTTP server: retries, 429 handling, gzip/deflate, rate limit.
- Live smoke tests, tagged and excluded from the default `./gradlew test` run.
- Each known bug gets a failing test before it is fixed.
- Contract tests (first layer of drift detection, see Phase 4):
  - strict-parse every ownership fixture with `FAIL_ON_UNKNOWN_PROPERTIES` on;
  - compare the element names in `specs/ownership-xml-*/schemas` with the model's fields,
    so dropping in a new spec version shows exactly what to add.
- These tests specify behaviour for the Kotlin rewrite.
- Result: `./gradlew test` runs 80 offline tests in ~7 s; `./gradlew liveTest` runs 6 smoke
  tests; `./gradlew knownBugTest` runs 12 tests that fail until Phase 3. Details in the
  findings log.

### Phase 3 — Fix bugs (Java)

Make `./gradlew knownBugTest` pass, removing each test's `known-bug` tag as its fix lands, and
write the tests that need an API change alongside their fixes. Behaviour fixes only;
renaming and restructuring wait for Phase 5.

Result: every known bug is fixed and no `known-bug` tags remain; `./gradlew test` runs
111 offline tests, `./gradlew liveTest` 6, all green. Behaviour changes are listed in the
findings log (Phase 3).

### Phase 4 — Spec drift monitoring

Phase 1 found EDGAR had changed silently (new XML schemas, new JSON fields). Notice the next
change within days, not years. Comes after Phase 3 so the check isn't noisy with known bugs,
and before Phase 5 so it protects the rewrite.

- **CI on every push/PR** (GitHub Actions): `./gradlew test`, offline only.
- **Live drift check** — a `driftCheck` Gradle task (tests tagged `drift`):
  - sample recent data from every source: tickers, a few submissions, latest feed,
    daily page, current quarter's `master.idx`, ~50 recent Form 3/4/5 filings across types;
  - parse strictly, collecting instead of failing: unknown fields/elements (via a Jackson
    `DeserializationProblemHandler`), unseen `schemaVersion` values, header-layout or index
    format changes, parse errors;
  - write a Markdown report and save each offending raw response (candidate new fixtures).
- **Spec watcher**: read https://www.sec.gov/submit-filings/technical-specifications and
  compare the listed Ownership spec version with the one in `specs/` (record known versions
  and known `schemaVersion` values in a small `specs/versions.json`).
- **Scheduled workflow** (weekly + manual trigger) runs both and opens or updates a single
  GitHub issue (label `spec-drift`) with the report; closes nothing automatically.
- **Runbook** in `CLAUDE.md`: drift reported → add the raw response as a fixture, refresh
  `specs/`, extend the model, contract tests go green, record it in the findings log.
- Needs: a repository secret with an EDGAR User-Agent (`Name contact@domain`) for CI requests.

Result: `ci.yml` (offline tests on push/PR) and `drift.yml` (weekly, Tuesdays 06:00 UTC, and
manual). `./gradlew driftCheck` makes about 70 requests in ~15 s. Code in
`src/test/java/com/alexkouzel/drift/`; the runbook is in `CLAUDE.md`. Details in the
findings log. Before the workflows run on GitHub: push `main` and add the
`EDGAR_USER_AGENT` secret.

### Phase 5 — Kotlin rewrite and redesign

Redesign rather than port line by line. Target structure: [ARCHITECTURE.md](ARCHITECTURE.md);
code rules: [CONVENTIONS.md](CONVENTIONS.md) (naming, comments, Kotlin style, tests).

Steps:

1. Build skeleton: modules `lib` and `drift`, Kotlin DSL, version catalog, `build-logic`
   convention plugin, source sets `test`, `testFixtures`, `contractTest`, `liveTest`;
   ktlint, detekt, explicit API mode, wired into CI.
2. Move fixtures to `lib/src/testFixtures/resources/edgar/` (as is, still frozen) and write
   `testFixtures` (`Fixtures`, `FixtureHttp`, `LocalServer`, `liveClient()`).
3. Port the tests area by area, then the code to make them pass: `http`, `company`,
   `filing`, `ownership` (model plus `ownership/xml`), then `SecClient`. Contract tests
   follow `ownership`.
4. Unknown-field listener on `SecClient`; port the drift check into the `drift` module on
   top of the public API; point `drift.yml` at it.
5. Remove the Java sources and Lombok; update CI and `CLAUDE.md` commands.

Progress (2026-10-23): step 1 done. The Java project moved to `legacy/` unchanged (its
tests read `../specs`); `lib` and `drift` are empty Kotlin modules on the `sec.kotlin`
convention plugin; CI runs `./gradlew check`. Versions: Kotlin 2.4.21, Jackson 3.2.3
(`tools.jackson`), JUnit 6.1.3, ktlint 1.8.0 (plugin 14.2.0), detekt 2.0.0-alpha.6; the
stable detekt 1.23 doesn't support Kotlin 2.4. Verified with throwaway files: each source
set compiles and runs under its task and sees `main`'s `internal` code; explicit API mode,
ktlint, detekt's package-vs-directory rule and the `known-bug` split all fail as they should.

Progress (2026-10-23): step 2 done except `liveClient()`, which comes with its first user,
`SecClient`; `LocalServer` came with `http`. Fixtures moved to `lib/src/testFixtures/resources/edgar/`
unchanged; `legacy` reads them from there. `Fixtures` and `FixtureHttp` are in `testFixtures`;
`test`, `contractTest` and `liveTest` are also associated with `testFixtures`, since
`FixtureHttp` implements the internal `Http` and so must be internal itself.

Progress (2026-10-23): step 3, `filing` done, ahead of the rest of `http` and of `company`.
It needed only a seam from `http`, which now exists: `Http` (`get(url)` → bytes, plus
`get(url, parse)`, which adds the URL to parse errors), `Mapping.kt` (shared Jackson mappers that
ignore unknown fields) and `SecException` (`Http`, `Parse`).

- API: `Filings.latest(count = 40, form, start)`, `daily(daysAgo = 0, form)`,
  `quarter(year, quarter)`, `ofCompany(cik)`, each `List<FilingRef>`; `latest` accepts the
  feed's counts (10, 20, 40, 80, 100), `quarter` 1993 onward and quarters 1–4, else
  `IllegalArgumentException`. `FilingRef(accessionNumber, cik, formType, filedOn)`;
  `FormType(value)` with constants `F3` … `F5_A`, `F8_K`, `F10_K`, `F10_Q` and `isAmendment`.
- `FilingRef.cik` is neutral (fixes the known limitation below): the Issuer's or Subject's CIK
  from the latest feed, the requested CIK from `ofCompany`, else the first one listed. A `Long`,
  as a CIK has up to 10 digits.
- One internal file per source (`LatestFeed`, `DailyList`, `QuarterlyIndex`, `Submissions`)
  builds the URL and parses the response; `Filings` filters by exact form type.
- Tests: 34 in `test/filing/`, from `FileRefLoaderTest` plus new ones (empty feed, entry without
  an accession number, page without the list, wrong column count, submission columns of unequal
  length, parse error names the URL). Removing the role preference or the form filter fails them.
- detekt allows up to 5 destructured entries (regex groups, index columns).

Progress (2026-10-23): step 3, `http` done: `EdgarHttp`, `RateLimiter`, and `LocalServer`
in `testFixtures`. 23 tests in `test/http/`, from `EdgarClientTest` and `RateLimiterTest`
(the JSON and XML mapping cases now test `Mapping.kt`), plus `Retry-After` as an HTTP date
and the URL in parse errors. Behaviour is the Java client's after Phase 3, except:

- the body is read inside the attempt, so a connection dropped mid-body is retried too
  (the Java client read it after the retry loop);
- a closed client fails at once instead of retrying;
- the status code handler is gone (in this repository only tests used it); `SecClient`
  can add a listener if users need one;
- no `Content-Type` header on GET requests.

Progress (2026-10-23): step 3, `company` done: `Companies.all()` returns
`Company(cik, name, ticker, exchange)`, `exchange` null where EDGAR gives none (the Java
model allowed it too; not checked against live data, as this session can't reach sec.gov).
`Tickers` finds columns by the names in `fields`, so a reordered or renamed field fails with
`SecException.Parse` instead of being misread. 6 tests, from `SecCompanyLoaderTest` plus
field order, null exchange, missing field, non-numeric CIK.

jackson-module-kotlin applies a property's non-null element type to nested collections too:
`List<List<Any?>>` rejects a `null` inside an inner list. `Tickers` reads each row as a
`JsonNode` instead; keep this in mind for `ownership/xml`.

Progress (2026-10-23): step 3, `ownership` done (contract tests next). `Ownership.get(ref)`
returns an `OwnershipFiling`; it rejects a ref whose form type isn't 3, 4 or 5 (or an
amendment) with `IllegalArgumentException`.

- Model: `OwnershipFiling` (header values, issuer, owners, transactions, holdings,
  footnotes, signatures, flags), `Owner.kt` (`Issuer`, `ReportingOwner`, `Address`,
  `Relationship`, `Signature`), `Transaction.kt` (`Transaction`, `Holding`, `Derivative`,
  `Coding`, `TransactionCode`), `Footnote.kt` (`Footnote`, `Footnoted<T>`). The XML's nesting
  is flattened: `transaction.shares: Footnoted<Double>?`.
- Both tables share `Transaction` and `Holding`; `derivative: Derivative?` holds a derivative
  security's exercise price, dates and underlying security. Lists are in filing order,
  non-derivative first.
- `Coding.value` keeps the raw transaction code; `Coding.code` is the `TransactionCode`, null
  when unknown. `TransactionCode` keeps the Java codes (the XSD's 18 plus `V` and `K`), some
  renamed after the SEC's descriptions: `F` `TAX_OR_EXERCISE_PAYMENT`, `U` `TENDER_OF_SHARES`,
  `G` `GIFT`, `W` `INHERITANCE`, `C` `CONVERSION`, `E`/`H` `EXPIRATION_SHORT`/`_LONG`,
  `O`/`X` `EXERCISE_OUT_OF_THE_MONEY`/`_IN_THE_MONEY`.
- `ownership/xml` mirrors the XSD with every leaf as text; `Values.kt` converts numbers,
  dates and flags and fails with `SecException.Parse` on a malformed one. An element with
  neither value nor footnote is null. Repeated elements keep `@JsonMerge` (with `@field:`);
  removing it from `nonDerivativeHolding` fails the interleaving test.
- Fixes over the Java parser: line breaks inside the XML are kept (Java deleted every
  newline, gluing words of multi-line footnotes: "effect ofcorrecting" in
  0001437749-22-017271); `document` is the `<FILENAME>` of the document holding the XML,
  not the first one in the filing; accession number, form type and filed date always come
  from the header.
- Tests: 35 in `test/ownership/`, from `OwnershipDocumentLoaderTest` plus the URL, the form
  type check, a missing `<XML>` block, unknown transaction codes and footnote line breaks.

Progress (2026-10-23): ownership contract tests done (`contractTest/ownership/`, 30 tests,
from `OwnershipContractTest`): every fixture parses strictly; XML element counts match the
model (owners, signatures, footnotes, both tables, footnote IDs); every XSD element is a
property of `ownership/xml` (removing `rptOwnerCcc` fails it, naming the element); every XSD
transaction code is in `TransactionCode`. `FeedContractTest` waits for step 4 (see below).

Build fixes found on the way: `check`'s detekt only analysed `main` and `test` (it now reads
all of `src/`, and test names and magic numbers are allowed in every test source set), and
the test suites name `kotlin-test-junit5`, which the Kotlin plugin infers for `test` only.

Progress (2026-10-23): step 3 done. `SecClient(userAgent, baseRetryDelay, maxRetryDelay)`
(`@JvmOverloads`) wires `companies`, `filings` and `ownership` over one `EdgarHttp` and closes
it; a User-Agent without `@` is rejected. `liveClient()` in `testFixtures` reads
`EDGAR_USER_AGENT`. `liveTest` holds `SecClientLiveTest` (port of `EdgarLiveTest`) and
`JavaLiveTest`, which calls the main paths from Java as CONVENTIONS asks. Both compile; they
have not passed yet, as the session that wrote them could not reach sec.gov (every request
was refused by its network policy, after building the right URLs). Run `./gradlew liveTest`
before step 5.

Offline totals: `lib` 101 unit tests and 30 contract tests, `legacy` 123, all green.

Live run (2026-10-23, network access added): `./gradlew liveTest` passes, 7 Kotlin tests and
the Java ones. Live company tickers: 164 of 10,429 rows have a null `exchange`, so the
nullable field is needed.

Progress (2026-10-23): step 4 done.

- `SecClient(..., unknownFields)` takes an `UnknownFieldListener`, told the URL and the field
  (`Issuer.issuerNewThing`) of everything EDGAR sends that the library doesn't know; parsing
  carries on. A Jackson problem handler on the shared mappers reports to the listener of the
  `Http.get` call running on that thread. New ticker columns are reported too.
- Decision (simplest, one mechanism): wire classes declare every field EDGAR sends, unused
  ones as `Any?`, instead of comparing key sets. The submissions JSON and latest feed classes
  were widened again. Contract tests collect unknown fields the same way and assert none;
  `FeedContractTest` is ported (submissions, older submissions, latest feed, tickers). The
  strict mapper in `OwnershipContractTest` was replaced: the problem handler would have
  made `FAIL_ON_UNKNOWN_PROPERTIES` pass silently.
- `SecException.url` names the request that failed or whose response didn't parse.
- `drift` module: `Main`, `DriftCheck` (same samples as the Java check, through `SecClient`;
  raw responses are fetched again only for findings), `Report`, `SpecPage`, `KnownVersions`;
  11 unit tests. `./gradlew :drift:driftCheck` (exit code 1 on findings); `drift.yml` runs it
  and reads `drift/build/drift/`.
- Request timeout raised from 5 s to 30 s: the first live drift run timed out on the latest
  feed, which EDGAR answered in 11–19 s (curl) while busy.

First live drift run: spec 5.5 current; tickers, four submissions (with every older file),
both quarterly indexes and 46 ownership filings (`X0607` 14, `X0609` 32; no 5/A in 2026 Q3
or Q4) clean, no unknown fields. The latest feed passed once the timeout was raised; the
daily list failed both runs with HTTP 503 and 30 s timeouts, as did curl (503 after 78 s), so
EDGAR's `cgi-bin/current` was down, not drifted. Rerun before trusting a `request failed`.

Next: step 5.

Wire classes map only the fields the library uses (the Java ones mapped every field), so
strict parsing of them can't spot EDGAR additions. Step 4 decides how the drift check sees
additions to the submissions JSON and the latest feed: full wire classes in `drift`, or comparing
key sets.

Renames (see the glossary): `FileRef` → `FilingRef`, `FileType` → `FormType` (keeps raw
values), `File`/`FileF345`/`FileF3..5` → `OwnershipFiling`, `LatestFilesLimit` → a validated
`count`, `loadFiscalQuarter` → `quarter(year, quarter)`.

Open questions, defaults assumed in ARCHITECTURE.md until decided:

- Java callers first-class? Default: yes, blocking API usable from Java.
- Scope: keep the current sources, or add XBRL company facts, 13F, 10-K? Default: keep;
  the area packages leave room to add them.
- Coroutines (`suspend`) alongside the blocking API? Default: no.

### Phase 6 — Documentation

Write the full `CLAUDE.md` and README for the Kotlin code.

## Decisions

- **Bugs are fixed in Java, renaming/restructuring happens in Kotlin.** Tests prove each
  fix before the port; redesigning the Java API right before a rewrite would be done twice.
- **Tests before fixes and before the rewrite.** Fixture tests are the behavioural spec.
- **Tolerant at runtime, strict in tests.** The library ignores unknown EDGAR fields so users
  don't break; contract tests and the scheduled drift check parse strictly so we notice.
- **Conventions are set for Kotlin, not retrofitted to Java.** `docs/ARCHITECTURE.md` and
  `docs/CONVENTIONS.md` define the target; the Java code isn't restructured to match, since
  Phase 5 replaces it. Tools enforce what they can, prose covers the rest.
- **One source set per kind of test.** Unit tests mirror `main`; support code lives only in
  `testFixtures`; contract and live tests have their own folders; the drift check is a
  separate module, not a test.
- **Comments are a last resort.** Names and structure carry the meaning; comments only for
  EDGAR quirks and reasons, KDoc only where a public signature leaves a question.
- **Commits follow the Angular convention**: `type(scope): subject` with types
  `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.

## Known bugs

Each open bug gets a test tagged `known-bug` (excluded from `./gradlew test`; run
`./gradlew knownBugTest`). Fixing one means making its test pass and removing the tag.

Fixed:

- [x] Gradle 8.5 cannot run on JDK 25; no toolchain pinned. (Phase 0)
- [x] Mappers fail on unknown properties, so any new EDGAR field breaks parsing. (Phase 1)
- [x] Tests depended on live data and only checked the first result. (Phase 2)
- [x] `deflate` responses were re-compressed instead of inflated. (Phase 3)
- [x] HTTP 429 slept 30–120 min on the calling thread. (Phase 3)
- [x] No backoff after I/O errors; every non-200 status was retried. (Phase 3)
- [x] Mapping errors surfaced as `HttpRequestException`. (Phase 3)
- [x] Each `RateLimiter` leaked a scheduler thread; client not closeable. (Phase 3)
- [x] Type filters were EDGAR prefix matches (2 Form 4s in 100 "type=4" entries). (Phase 3)
- [x] Latest feed and daily page returned a filing once per related CIK. (Phase 3)
- [x] `FileRef.issuerCik` could be the reporting owner's CIK on the latest feed. (Phase 3)
- [x] Interleaved repeated XML elements dropped all but the last group. (Phase 3)
- [x] Only one `footnoteId` per value kept; `transactionCoding` footnotes lost. (Phase 3)
- [x] `ListUtils.removeDups` reordered by hash code. (Phase 3)
- [x] Positional parsing of the latest feed, daily page and quarterly index. (Phase 3)
- [x] `loadCompany` returned only `filings.recent`; older filings in `filings.files` ignored. (Phase 3)
- [x] `OwnershipDocumentParser` read header lines 3/4/7 by position. (Phase 3)
- [x] `TestEdgarClient` lived in `src/main` and shipped with the library. (Phase 3)
- [x] `FileUrlBuilder.buildF345` hard-coded a stale stylesheet path; nothing called it. (Phase 3)

Open: none.

Known limitations, for the Kotlin redesign:

- [x] On the daily page and quarterly index the CIK's role is unknown; `loadCompany(cik)` sets
  `issuerCik` to the requested CIK even when it's a reporting owner. `FileRef.issuerCik`
  should become a role-qualified or neutral field (see `docs/CONVENTIONS.md`). Kotlin:
  `FilingRef.cik` is neutral. (Phase 5)

## Findings log

### Phase 1 — 2026-10-23

All endpoints still exist and return 200. Formats checked against the parsers:

| Source | Status | Notes |
|--------|--------|-------|
| Company tickers | OK | Format unchanged. |
| Submissions JSON | **Broke, fixed** | EDGAR added `filings.recent.isXBRLNumeric`; strict mapping failed. |
| Latest Atom feed | Works, wrong results | Prefix type filter and per-CIK duplicates (see known bugs). |
| Daily page | Works, wrong results | Same as latest feed. `<hr>` layout unchanged. |
| Quarterly `master.idx` | OK | Still 11 header lines; 2026 Q4 (57,414 rows) and 2022 Q3 (237,784) parse. |
| Filing `.txt` header | OK | Accession/type/filed-date lines still at positions 3/4/7. |
| Form 3/4/5 XML | **Broke, fixed** | Every 2026 filing failed: schemas `X0607` (Form 3) / `X0609` (Forms 4, 5) added `issuerForeignTradingSymbol`, `rptOwnerNonUSAddressFlag`, `rptOwnerNonUSStateTerritory`, `rptOwnerCountry`. 2022 filings parsed fine. |

Cause: EDGAR changed (new fields), not code drift. The old spec in `specs/` predated
schema X06xx; replaced with Ownership XML Technical Specification v5.5 (March 2026).
Diffing its XSDs against the model showed exactly the four new elements, nothing else missing.

Blocker fixes applied:
- Added the four ownership elements and `isXBRLNumeric` to the model.
- Disabled `FAIL_ON_UNKNOWN_PROPERTIES` on all mappers so future additions don't break parsing.

Verification: existing suite 12/12 green; a throwaway probe parsed 204 sampled Form 3, 3/A, 4,
4/A, 5, 5/A filings (2022 Q3 and 2026 Q4) with a strict mapper and matching header fields.
Phase 2 should keep a strict-mapper fixture test so new schema elements are noticed.

### Phase 2 — 2026-10-23

Test layout (`src/test/java/com/alexkouzel/`):

| Area | Classes | How |
|------|---------|-----|
| Loaders and parsers | `company/`, `file/reference/`, `file/type/f345/` | `testing.FixtureClient` serves fixtures by exact URL, so URL building is checked too. Expected values were extracted with an independent Python parser, not from the Java output. |
| Contracts | `contract/OwnershipContractTest`, `contract/FeedContractTest` | Strict parsing of every fixture; XML element counts vs model; XSD elements vs Jackson's view of the model; XSD transaction codes vs `TransactionCode`. Checked that removing a model field fails with the element's name. |
| HTTP client | `client/EdgarClientTest` | `testing.LocalHttpServer` (JDK `HttpServer`, no extra dependency): headers, gzip, JSON/XML mapping, retries, dropped connections, 10 requests/second. |
| Live | `live/EdgarLiveTest` (tag `live`) | Every source answers and parses. |

New bugs found while writing tests: interleaved repeated elements lose data, multiple
`footnoteId`s collapse to one, `removeDups` reorders, `loadCompany` ignores older filings.
One suspected bug was disproved (an empty daily page parses to an empty list).

JUnit upgraded 5.8.1 → 6.1.3.

### Phase 3 — 2026-10-23

Commits `c839732` (client), `4cec510` (filings), `d4634d9` (ownership), `29ad583` (older
submissions), `8cf1b7c` (test client), `ca75ae4` (header labels), `f38b239` (cleanup).
Behaviour changes, for the README and the Kotlin port:

- Retries: I/O errors, 429 and 5xx, exponential backoff (1 s base, 1 min cap, configurable
  via `EdgarClient(userAgent, handler, baseRetryDelay, maxRetryDelay)`); other 4xx fail at
  once. 429 honours `Retry-After` (capped) and pauses the rate limiter.
- `RateLimiter` is a thread-free sliding window (≤ 10 requests in any 1 s); `HttpDataClient`
  is `AutoCloseable`. `StatusCodeException` removed.
- `loadJson`/`loadXml`/`SecCompanyLoader.load`/`loadCompany` throw `ParsingException` on
  mapping errors.
- Type-filtered loaders return exactly that type (may be fewer than `count`); results are
  unique per accession number; latest feed keeps the Issuer's/Subject's CIK.
- Feed parsers match patterns and fail loudly on unrecognized rows; the index parser finds
  rows after the dashed separator.
- `FootnoteValue`/`FootnoteEdgarDate`/coding classes expose `getFootnoteIds()` (list)
  instead of `getFootnoteId()`; empty date elements map to `null`.
- `loadCompany` returns every filing: `filings.recent`, then each older file in
  `filings.files` (one extra request per file; Apple has one, 1,268 filings).
- `OwnershipDocumentParser.parse(String)` finds header values by label inside
  `<SEC-HEADER>` and throws `ParsingException` when one is missing.
- `TestEdgarClient` moved to the test sources; it reads `EDGAR_USER_AGENT`.
- `FileUrlBuilder.buildF345` removed.
- JDK `HttpClient` silently retries a dropped connection once by itself; tests account
  for it.

### Phase 4 — 2026-10-23

Drift check (`drift/EdgarDriftTest`, tag `drift`), one run over every source:

| Source | Sample | Checks |
|--------|--------|--------|
| Spec page | `submit-filings/technical-specifications` | Ownership section version and every linked `ownershipxmltechspec-v*.zip` (drafts too) vs `specs/versions.json` |
| Company tickers | full file | `fields` is `[cik, name, ticker, exchange]`, no extra keys, every row matches |
| Submissions | Apple, Tesla, CIK 946581, Tim Cook (insider); newest older file of each | unknown fields, filing columns of equal length |
| Latest feed | 40 entries, with and without `type=4` | unknown elements, parser |
| Daily list | 1 day ago, with and without `q3=4` | parser (an empty weekend page is fine) |
| Quarterly index | current quarter; previous too when a form type is short | column line, parser |
| Ownership filings | newest 20 × 4, 10 × 3, 6 × 5, 6 × 4/A, 4 × 3/A, 4 × 5/A from the index | unknown elements, parser, `schemaVersion` known, header form type = index form type |

Unknown fields are collected with a Jackson `DeserializationProblemHandler`
(`UnknownFieldCollector`) on mappers set up like the library's, so one run lists every
addition. Every failure is a finding, so a broken source doesn't hide the others.

First run: no drift. Spec 5.5 is current; recent filings carry `X0607` (14) and `X0609`
(32); no 5/A was filed in 2026 Q3 or Q4 so far. Removing `X0609` from `versions.json` gave
32 findings with their raw filings saved, as expected. The issue step was dry-run with a
stub `gh` (create when none is open, else edit and comment). The workflows themselves
haven't run on GitHub yet.
