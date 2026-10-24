# CLAUDE.md

Kotlin library for SEC EDGAR data (`com.github.alexkouzel:sec-api` on JitPack): companies, filing references and
insider ownership filings (Forms 3, 4, 5), behind one blocking, Java-friendly `SecClient`.

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (layout and design) and
[docs/CONVENTIONS.md](docs/CONVENTIONS.md) (EDGAR glossary, naming, comments, style, tests)
before changing code, and use the glossary's terms.

## Layout

- `src/main` — the library. `src/test` — offline unit tests, their helpers (`Fixtures`,
  `FixtureHttp`, `LocalServer`) and frozen EDGAR responses. `src/liveTest` — the library against
  live EDGAR.
- `specs/` — the SEC's Ownership XML specification and XSDs.
- `config/detekt.yml` — detekt's differences from its defaults; versions in
  `gradle/libs.versions.toml`, kept current by Dependabot (`.github/dependabot.yml`).
- `jitpack.yml` — JitPack builds each release from its git tag; there is no publish step.

## Commands

- `./gradlew check` — offline: unit tests, ktlint, detekt. Must stay green; CI runs it on every
  push and pull request.
- `./gradlew ktlintFormat` — fixes formatting.
- `./gradlew liveTest` — against live EDGAR; reads the User-Agent from env `EDGAR_USER_AGENT`
  (`Name contact@domain`).

## Fixtures

`src/test/resources/edgar/` holds raw EDGAR responses. They are frozen: tests assert
exact values, so add new files (listed in that folder's README), never edit one. Expected
values in tests come from the raw data, not from the code's output.

## Managing the project

Two project skills (`.claude/skills/`) cover the recurring jobs:

- `/edgar-change` — the daily `.github/workflows/live.yml` run failed (an issue labelled
  `edgar-change` opened): find what broke, save the response as a fixture, fix it test-first.
  The workflow needs the repository secret `EDGAR_USER_AGENT`.
- `/release` — work out the next version from the commits since the last release, verify
  against live EDGAR, tag it, create a GitHub release, and check that JitPack builds it.

## Commits

Angular convention: `type(scope): subject`, imperative, lowercase, no trailing period.
Types: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.
Example: `fix(http): inflate deflate-encoded responses`.

`/release` derives the version from these: `feat` is a minor release, everything else a patch.
Mark a change that breaks the public API with `!` (`feat(filing)!: rename latest to recent`)
or a `BREAKING CHANGE:` line in the body; it makes the next release major.
