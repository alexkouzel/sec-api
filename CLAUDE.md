# CLAUDE.md

Kotlin library for SEC EDGAR data (`com.alexkouzel:sec-api`): companies, filing references
and insider ownership filings (Forms 3, 4, 5), behind one blocking, Java-friendly `SecClient`.

## Read first

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): packages, public API, test layout, and how
  unknown EDGAR fields are reported (the core of drift detection).
- [docs/CONVENTIONS.md](docs/CONVENTIONS.md): the EDGAR glossary (CIK roles, accession
  number, filing vs form vs document) and the naming, comment, style and test rules. Use its
  terms in code, tests and docs.

## Modules

- `lib/` — the library. Source sets: `main`, `test` (unit, mirrors `main`), `testFixtures`
  (`Fixtures`, `FixtureHttp`, `LocalServer`, `liveClient()`, frozen EDGAR fixtures),
  `contractTest` (fixtures and XSDs vs the model), `liveTest` (real EDGAR, Kotlin and Java).
- `drift/` — the drift-check application, built only on `lib`'s public API.
- `build-logic/` — the `sec.kotlin` convention plugin (Java 21 toolchain, ktlint, detekt over
  every source set, JUnit); versions in `gradle/libs.versions.toml`; detekt settings in
  `config/detekt.yml`.
- `specs/` — the SEC's Ownership XML spec and XSDs, and `specs/versions.json` (known spec and
  `schemaVersion`s).

## Commands

- `./gradlew check` — offline, every module: unit and contract tests, ktlint, detekt. Must
  stay green; CI runs it on every push and pull request.
- `./gradlew ktlintFormat` — fixes formatting.
- `./gradlew liveTest` — smoke tests against real EDGAR.
- `./gradlew :drift:driftCheck` — samples live EDGAR and the SEC spec page; writes
  `drift/build/drift/report.md`, saves offending responses under `drift/build/drift/raw/`, and
  fails when it finds anything.
- `./gradlew knownBugTest` — tests tagged `known-bug`, each naming the GitHub issue of an open
  bug; they fail until it's fixed, then the tag goes.
- `scripts/publish.sh <version>` — publishes `lib` to GitHub Packages (`GPR_USERNAME`,
  `GPR_TOKEN`).

Live tasks read the User-Agent from env `EDGAR_USER_AGENT` (`Name contact@domain`, as EDGAR's
fair access policy requires). EDGAR allows 10 requests per second; `SecClient` keeps under it.

## Fixtures

`lib/src/testFixtures/resources/edgar/` holds raw EDGAR responses, frozen: tests assert exact
values, so add new files (listed in that folder's README) and never refresh one in place.
Expected values in tests come from the raw data, extracted independently, not from the code.

## CI

- `.github/workflows/ci.yml` — `./gradlew check` on every push and pull request.
- `.github/workflows/drift.yml` — `./gradlew :drift:driftCheck` weekly (Tuesdays) and on
  demand; on findings it opens or updates one issue labelled `spec-drift` with the report and
  uploads `drift/build/drift/` as the `drift-report` artifact. It never closes the issue.
  Needs the repository secret `EDGAR_USER_AGENT`.

## Drift runbook

When the drift check reports something:

1. Download the run's `drift-report` artifact. Each finding names its raw response. A
   `request failed` finding alone is usually EDGAR being down; rerun before acting on it.
2. Add the raw response as a new fixture under `lib/src/testFixtures/resources/edgar/<source>/`
   and list it in that folder's README.
3. New spec version: download it from the spec page into `specs/ownership-xml-v<version>/`,
   update `specs/README.md` and `ownershipSpec` in `specs/versions.json`. Unseen
   `schemaVersion`: add it to `schemaVersions` once its filings parse fully.
4. New fields or elements: add them to the wire classes (every field EDGAR sends is declared,
   read or not) and to the model if users need them. The contract tests
   (`OwnershipContractTest`, `FeedContractTest`) must fail on the new fixture first, then pass.
5. Format change (parse error, layout changed): fix the parser with a test on the new fixture.
6. Explain what changed on EDGAR in the fixing commit and on the `spec-drift` issue, run
   `./gradlew :drift:driftCheck` until clean, then close the issue by hand.

## Commits

Angular convention: `type(scope): subject`, imperative, lowercase, no trailing period.
Types: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.
Example: `fix(http): inflate deflate-encoded responses`.
