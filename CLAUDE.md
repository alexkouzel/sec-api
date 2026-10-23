# CLAUDE.md

Kotlin library for SEC EDGAR data (`com.alexkouzel:sec-api`): companies, filing references and
insider ownership filings (Forms 3, 4, 5), behind one blocking, Java-friendly `SecClient`.

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (layout and design) and
[docs/CONVENTIONS.md](docs/CONVENTIONS.md) (EDGAR glossary, naming, comments, style, tests)
before changing code, and use the glossary's terms.

## Layout

- `src/main` — the library. `src/test` — offline unit tests. `src/testFixtures` — test support
  and frozen EDGAR responses. `src/liveTest` — the library against live EDGAR.
- `specs/` — the SEC's Ownership XML specification and XSDs.
- `config/detekt.yml` — detekt's differences from its defaults; versions in
  `gradle/libs.versions.toml`.

## Commands

- `./gradlew check` — offline: unit tests, ktlint, detekt. Must stay green; CI runs it on every
  push and pull request.
- `./gradlew ktlintFormat` — fixes formatting.
- `./gradlew liveTest` — against live EDGAR; reads the User-Agent from env `EDGAR_USER_AGENT`
  (`Name contact@domain`).
- `scripts/publish.sh <version>` — publishes to GitHub Packages (`GPR_USERNAME`, `GPR_TOKEN`).

## Fixtures

`src/testFixtures/resources/edgar/` holds raw EDGAR responses. They are frozen: tests assert
exact values, so add new files (listed in that folder's README), never edit one. Expected
values in tests come from the raw data, not from the code's output.

## When the live check fails

`.github/workflows/live.yml` runs `./gradlew liveTest` daily (retrying once ten minutes later)
and, on failure, opens or comments on an issue labelled `edgar-change`. It needs the repository
secret `EDGAR_USER_AGENT`. To adapt:

1. Open the run linked from the issue. Each failed test names the URL that broke and why
   (`SecException.Parse`, or a value that came back empty).
2. If every failure is `SecException.Http`, EDGAR was down: rerun the workflow before acting.
3. Save that response as a new fixture, e.g.
   `curl -A "$EDGAR_USER_AGENT" --compressed -o src/testFixtures/resources/edgar/ownership/<accession>.txt <url>`,
   and list it in the fixtures README.
4. Write a unit test on the new fixture that fails, then fix the parser or the model until it
   passes. If the SEC published a new Ownership spec, add it to `specs/` (see its README) and
   `OwnershipDocumentTest` names what's missing.
5. Run `./gradlew check` and `./gradlew liveTest`, release, and close the issue.

## Commits

Angular convention: `type(scope): subject`, imperative, lowercase, no trailing period.
Types: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.
Example: `fix(http): inflate deflate-encoded responses`.
