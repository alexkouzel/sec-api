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

- On the daily page and quarterly index the CIK's role is unknown; `loadCompany(cik)` sets
  `issuerCik` to the requested CIK even when it's a reporting owner. `FileRef.issuerCik`
  should become a role-qualified or neutral field (see `docs/CONVENTIONS.md`).

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
