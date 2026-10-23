# CLAUDE.md

Java library for SEC EDGAR data, being restored and rewritten in Kotlin.

**Read [docs/ROADMAP.md](docs/ROADMAP.md) first.** It holds the current phase, decisions,
known bugs, and findings. Update it in the commit that completes or changes a phase.

**Use the terms in [docs/CONVENTIONS.md](docs/CONVENTIONS.md)** (EDGAR glossary: CIK roles,
accession number, filing vs form vs document) in code, tests and docs.

## Tests

- `./gradlew test` — offline: fixtures, contract tests, local HTTP server. Must stay green.
- `./gradlew liveTest` — tag `live`, hits real EDGAR.
- `./gradlew knownBugTest` — tag `known-bug`, fails until each bug in the roadmap is fixed;
  remove a test's tag when its fix lands.
- `./gradlew driftCheck` — tag `drift`, samples live EDGAR and the SEC spec page; writes
  `build/drift/report.md` and saves offending responses under `build/drift/raw/`.
- Live tasks read the User-Agent from env `EDGAR_USER_AGENT` (`Name contact@domain`).
- Fixtures in `src/test/resources/edgar/` are frozen (see its README); add new files,
  don't refresh existing ones. Specs and XSDs live in `specs/`.

## CI

- `.github/workflows/ci.yml` runs `./gradlew test` on every push and pull request.
- `.github/workflows/drift.yml` runs `./gradlew driftCheck` weekly (Tuesdays) and on demand;
  on findings it opens or updates one issue labelled `spec-drift`, with the report, and
  uploads `build/drift/` as the `drift-report` artifact. It never closes the issue.
  Needs the repository secret `EDGAR_USER_AGENT`.

## Drift runbook

When the drift check reports something:

1. Download the run's `drift-report` artifact. Each finding names its raw response.
2. Add the raw response as a new fixture under `src/test/resources/edgar/<source>/` and list
   it in that folder's README (fixtures are never refreshed in place).
3. New spec version: download it from the spec page into `specs/ownership-xml-v<version>/`,
   update `specs/README.md` and `ownershipSpec` in `specs/versions.json`. Unseen
   `schemaVersion`: add it to `schemaVersions` once its filings parse fully.
4. Extend the model for new fields or elements; the contract tests
   (`OwnershipContractTest`, `FeedContractTest`) must fail first, then pass.
5. Format change (parse error, layout changed): fix the parser with a test on the new fixture.
6. Record it in the findings log in `docs/ROADMAP.md`, run `./gradlew driftCheck` until
   clean, then close the issue by hand.

## Commits

Angular convention: `type(scope): subject`, imperative, lowercase, no trailing period.
Types: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.
Example: `fix(client): inflate deflate-encoded responses`.

This file is a placeholder; the full version is written in Phase 6.
