# Roadmap

Plan for bringing sec-api back to a working state and rewriting it in Kotlin.
Update this file in the same commit that completes (or changes) a phase.

## Status

| Phase | Goal | Status |
|-------|------|--------|
| 0 | Build compiles on a current JDK | Not started |
| 1 | Verify against live EDGAR, refresh specs | Not started |
| 2 | Automated tests (offline fixtures, mock client, opt-in live) | Not started |
| 3 | Fix known bugs in Java | Not started |
| 4 | Kotlin rewrite and redesign | Not started |
| 5 | Final `CLAUDE.md` and README | Not started |

## Phases

### Phase 0 — Build

- Pin a JDK toolchain (21) in Gradle; upgrade the Gradle wrapper and the Lombok plugin.
- No logic changes. Done when `./gradlew compileJava compileTestJava` succeeds.

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

- [ ] Gradle 8.5 cannot run on JDK 25; tests use `List.getFirst()` (Java 21) while the
      README claims Java 17; no toolchain pinned.
- [ ] `HttpDataClient` decodes `deflate` with `DeflaterInputStream` (compresses);
      should be `InflaterInputStream`.
- [ ] HTTP 429 sleeps 30–120 minutes on the calling thread; SEC guidance is ~10 minutes.
- [ ] Each `RateLimiter` starts a scheduler thread that is never shut down.
- [ ] Retries have no backoff except after non-200 status codes (none after I/O errors).
- [ ] Position-based parsing breaks silently on format changes:
      `DailyFeedParser` (HTML scraping), `IndexFeedParser` (skips 11 lines),
      `LatestFeedParser` (`split(" ")[4]`), `OwnershipDocumentParser` (header lines 3/4/7).
- [ ] `XmlMapper` fails on unknown properties, so any new Form 3/4/5 schema element breaks parsing.
- [ ] `TestEdgarClient` lives in `src/main` and ships with the library.
- [ ] Tests depend on live data (`loadDaysAgo(1)` fails on weekends) and have no offline fixtures.
- [ ] `FileUrlBuilder.buildF345` hard-codes the `xslF345X03` stylesheet path.

## Findings log

Filled in during Phase 1.
