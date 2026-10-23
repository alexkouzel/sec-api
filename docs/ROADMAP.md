# Roadmap

Plan for bringing sec-api back to a working state and rewriting it in Kotlin.
Update this file in the same commit that completes (or changes) a phase.

## Status

| Phase | Goal | Status |
|-------|------|--------|
| 0 | Build compiles on a current JDK | Done |
| 1 | Verify against live EDGAR, refresh specs | Done |
| 2 | Automated tests (offline fixtures, mock client, opt-in live) | Done |
| 3 | Fix known bugs in Java | Not started |
| 4 | Spec drift monitoring (CI, scheduled live check) | Not started |
| 5 | Kotlin rewrite and redesign | Not started |
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

### Phase 5 — Kotlin rewrite and redesign

Redesign rather than port line by line, porting the Phase 2 and 4 tests first and keeping them green.

First step — conventions, before any Kotlin code:

- Confirm the glossary's Kotlin names in `docs/CONVENTIONS.md` (the glossary itself
  applies from Phase 3 on).
- Write its Structure and Kotlin style sections together with the API design.
- Tooling, wired into the Phase 4 CI: `.editorconfig`, ktlint (formatting), detekt (lint),
  Kotlin explicit API mode (every public declaration is a deliberate choice).

Then:

- Packages by area: `client`, `company`, `filing`, `ownership`.
- Renames: `FileRef` → `FilingRef`, `FileType` → `FormType`, drop `File`/`FileF345`
  for a plain `OwnershipFiling`, replace `LatestFilesLimit` with a validated count.
- Single entry point (e.g. `SecClient` with `.companies`, `.filings`, `.ownership`).
- Token-bucket rate limiter, exponential backoff, 429 handling per SEC guidance,
  closeable client.
- Sealed error type instead of checked exceptions; keep the API usable from Java.
- Optional unknown-field listener on the client, so library users see drift in production
  too (tolerant parsing stays the default).
- Port the Phase 4 drift check along with the tests.
- Remove Lombok.

Open questions before starting: must Java callers stay first-class? Expand scope
(XBRL company facts, 13F, 10-K) or keep the current one?

### Phase 6 — Documentation

Write the full `CLAUDE.md` and README for the Kotlin code.

## Decisions

- **Bugs are fixed in Java, renaming/restructuring happens in Kotlin.** Tests prove each
  fix before the port; redesigning the Java API right before a rewrite would be done twice.
- **Tests before fixes and before the rewrite.** Fixture tests are the behavioural spec.
- **Tolerant at runtime, strict in tests.** The library ignores unknown EDGAR fields so users
  don't break; contract tests and the scheduled drift check parse strictly so we notice.
- **Conventions are set for Kotlin, not retrofitted to Java.** `docs/CONVENTIONS.md` holds
  the EDGAR glossary now; structure and style rules are written at the start of Phase 5 and
  enforced by tools where possible, prose only for what tools can't check.
- **Commits follow the Angular convention**: `type(scope): subject` with types
  `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.

## Known bugs

Each open bug has a test tagged `known-bug` (excluded from `./gradlew test`; run
`./gradlew knownBugTest`). Fixing one means making its test pass and removing the tag.

Fixed:

- [x] Gradle 8.5 cannot run on JDK 25; tests use `List.getFirst()` (Java 21) while the
      README claims Java 17; no toolchain pinned. (Phase 0)
- [x] Mappers fail on unknown properties, so any new EDGAR field breaks parsing. (Phase 1)
- [x] Tests depended on live data and only checked the first result. (Phase 2)

Open, with a failing test:

| Bug | Test |
|-----|------|
| `deflate` responses go through `DeflaterInputStream` (compresses instead of inflating) | `EdgarClientTest.decodesDeflateBody` |
| HTTP 429 sleeps 30–120 min on the calling thread; the SEC doesn't publish a lockout duration, so use a shorter, configurable backoff | `EdgarClientTest.recoversFromTooManyRequestsWithoutHanging` |
| Retries after I/O errors happen immediately, with no backoff | `EdgarClientTest.backsOffBeforeRetryingDroppedConnection` |
| `loadJson`/`loadXml` report mapping errors as `HttpRequestException`, not `ParsingException` | `EdgarClientTest.reportsInvalidJsonAsParsingError` |
| Type filters are EDGAR prefix matches (`type=4` also returns 424B2, 497, ...): 2 Form 4s in 100 latest entries, 434 of 1,231 on the daily page | `FileRefLoaderTest.Latest/DaysAgo.typeFilterReturnsOnlyThatType` |
| Latest feed and daily page list a filing once per related CIK (100 entries, 66 filings) | `FileRefLoaderTest.Latest/DaysAgo.returnsEachFilingOnce` |
| `FileRef.issuerCik` can be the reporting owner's CIK (the latest feed labels entries `(Issuer)`/`(Reporting)`; the daily page doesn't, so there the field is really "a related CIK") | `FileRefLoaderTest.Latest.issuerCikComesFromIssuerEntry` |
| Non-contiguous repeated XML elements (e.g. holding, transaction, holding — valid per XSD) keep only the last group, silently dropping data; seen in 1 of 14 fixtures | `OwnershipDocumentLoaderTest.interleavedHoldingsAreAllKept`, `OwnershipContractTest.modelKeepsEveryRepeatedElementWhenInterleaved` |
| `ListUtils.removeDups` (and public `FileRefUtils.removeDups`) reorders by hash code | `ListUtilsTest.removeDupsKeepsFirstSeenOrder` |

Open, test comes with the fix (needs an API change):

- [ ] A value can carry up to 99 `footnoteId`s (XSD), but `FootnoteValue` holds one; the rest
      are dropped. Fixture: `0001000045-22-000004` (`transactionShares` has F1 and F2).
- [ ] Each `RateLimiter` starts a scheduler thread that is never shut down; the client isn't
      closeable.
- [ ] `loadCompany` returns only `filings.recent` (up to 1,000); older filings listed in
      `filings.files` are ignored (the fixture lists 1,105 of them).

Open, no test (refactor or remove):

- [ ] Position-based parsing breaks silently on format changes: `DailyFeedParser` (HTML
      scraping), `IndexFeedParser` (skips 11 lines), `LatestFeedParser` (`split(" ")[4]`),
      `OwnershipDocumentParser` (header lines 3/4/7). Formats are covered by fixture tests,
      so a rewrite can be checked against them.
- [ ] `TestEdgarClient` lives in `src/main` and ships with the library.
- [ ] `FileUrlBuilder.buildF345` hard-codes the `xslF345X03` stylesheet path (spec v5.5 ships
      `X06`–`X09`); nothing calls it.

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
