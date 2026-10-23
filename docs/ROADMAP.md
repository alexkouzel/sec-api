# Roadmap

Plan for bringing sec-api back to a working state and rewriting it in Kotlin.
Update this file in the same commit that completes (or changes) a phase.

## Status

| Phase | Goal | Status |
|-------|------|--------|
| 0 | Build compiles on a current JDK | Done |
| 1 | Verify against live EDGAR, refresh specs | Done |
| 2 | Automated tests (offline fixtures, mock client, opt-in live) | Not started |
| 3 | Fix known bugs in Java | Not started |
| 4 | Kotlin rewrite and redesign | Not started |
| 5 | Final `CLAUDE.md` and README | Not started |

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
- These tests specify behaviour for the Kotlin rewrite.

### Phase 3 — Fix bugs (Java)

Make the Phase 2 suite pass. Behaviour fixes only; renaming and restructuring wait for Phase 4.

### Phase 4 — Kotlin rewrite and redesign

Redesign rather than port line by line, porting the Phase 2 tests first and keeping them green.

- Packages by area: `client`, `company`, `filing`, `ownership`.
- Renames: `FileRef` → `FilingRef`, `FileType` → `FormType`, drop `File`/`FileF345`
  for a plain `OwnershipFiling`, replace `LatestFilesLimit` with a validated count.
- Single entry point (e.g. `SecClient` with `.companies`, `.filings`, `.ownership`).
- Token-bucket rate limiter, exponential backoff, 429 handling per SEC guidance,
  closeable client.
- Sealed error type instead of checked exceptions; keep the API usable from Java.
- Remove Lombok.

Open questions before starting: must Java callers stay first-class? Expand scope
(XBRL company facts, 13F, 10-K) or keep the current one?

### Phase 5 — Documentation

Write the full `CLAUDE.md` and README for the Kotlin code.

## Decisions

- **Bugs are fixed in Java, renaming/restructuring happens in Kotlin.** Tests prove each
  fix before the port; redesigning the Java API right before a rewrite would be done twice.
- **Tests before fixes and before the rewrite.** Fixture tests are the behavioural spec.
- **Commits follow the Angular convention**: `type(scope): subject` with types
  `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.

## Known bugs

Found by reading the code (2026-10-23); confirm with tests in Phase 2.

- [x] Gradle 8.5 cannot run on JDK 25; tests use `List.getFirst()` (Java 21) while the
      README claims Java 17; no toolchain pinned.
- [ ] `HttpDataClient` decodes `deflate` with `DeflaterInputStream` (compresses);
      should be `InflaterInputStream`.
- [ ] HTTP 429 sleeps 30–120 minutes on the calling thread. The SEC's fair-access pages
      don't publish a lockout duration; pick a shorter, configurable backoff.
- [ ] Each `RateLimiter` starts a scheduler thread that is never shut down.
- [ ] Retries have no backoff except after non-200 status codes (none after I/O errors).
- [ ] Position-based parsing breaks silently on format changes:
      `DailyFeedParser` (HTML scraping), `IndexFeedParser` (skips 11 lines),
      `LatestFeedParser` (`split(" ")[4]`), `OwnershipDocumentParser` (header lines 3/4/7).
- [x] Mappers fail on unknown properties, so any new EDGAR field breaks parsing.
      Fixed in Phase 1 (blocker): see findings log.
- [ ] `TestEdgarClient` lives in `src/main` and ships with the library.
- [ ] Tests depend on live data and only check the first result's fields. Fixtures now exist
      (`src/test/resources/edgar/`); tests don't use them yet. Weekend behaviour of
      `loadDaysAgo` is unverified.
- [ ] `FileUrlBuilder.buildF345` hard-codes the `xslF345X03` stylesheet path; spec v5.5 ships
      `X06`–`X09` stylesheets.
- [ ] Form type filters are EDGAR prefix matches: `loadLatest(..., F4)` returned 2 Form 4s out
      of 100 entries (rest 424B2, 497, ...); `loadDaysAgo(1, F4)` returned 434 of 1231.
      Filter by exact type client-side.
- [ ] The latest feed and daily page list a filing once per related CIK (Filer, Issuer,
      Reporting): 100 entries held 66 unique filings. Results contain duplicates, and
      `FileRef.issuerCik` is sometimes the reporting owner's CIK.
- [ ] `HttpDataClient.loadType` reports JSON/XML mapping errors as `HttpRequestException`
      instead of `ParsingException`.

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
