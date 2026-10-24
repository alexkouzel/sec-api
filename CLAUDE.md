# CLAUDE.md

Kotlin library for SEC EDGAR data (`com.github.alexkouzel:sec-api` on JitPack): companies,
filing references and insider ownership filings (Forms 3, 4, 5), behind one blocking,
Java-friendly `SecClient`.

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (layout and design) and
[docs/CONVENTIONS.md](docs/CONVENTIONS.md) (EDGAR glossary, naming, comments, style, tests)
before changing code, and use the glossary's terms.

## Layout

- `src/main` — the library. `src/test` — offline unit tests, their helpers (`Fixtures`,
  `FixtureHttp`, `LocalServer`) and raw EDGAR responses in `resources/edgar/`, which are frozen
  (see Tests in CONVENTIONS.md). `src/liveTest` — the library against live EDGAR.
- `specs/` — the XSDs of the SEC's Ownership XML specification, which a unit test checks the
  parser against.
- `build.gradle.kts` — the whole build, with every version; Dependabot (`.github/dependabot.yml`)
  proposes updates. `.editorconfig` configures ktlint.
- `jitpack.yml` — JitPack builds each release from its git tag; there is no publish step.

## Commands

- `./gradlew check` — offline: unit tests and ktlint. Must stay green; CI runs it on every
  pull request and push to `main`.
- `./gradlew ktlintFormat` — fixes formatting.
- `./gradlew liveTest` — against live EDGAR; reads the User-Agent from env `EDGAR_USER_AGENT`
  (`Name contact@domain`).

## Managing the project

- When the daily `.github/workflows/live.yml` run fails, it opens an issue labelled
  `live-check-failed`; the `/fix-live-check` skill (`.claude/skills/`) finds what broke, saves
  the response as a fixture and fixes it test-first. The workflow needs the repository secret
  `EDGAR_USER_AGENT`.
- Releases are made by hand on GitHub. First set the new version in the README's installation
  snippet on `main`, then Releases → "Draft a new release", tag `v<version>` on `main`,
  "Generate release notes", publish. JitPack builds a release when it is first requested.
  The version follows the commits since the last tag (see Commits).

## Commits

Angular convention: `type(scope): subject`, imperative, lowercase, no trailing period.
Types: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.
Example: `fix(filing): read the daily list's new row layout`.

They decide the next version: a `feat` makes a minor release, anything else a patch. Mark a
change that breaks the public API with `!` (`feat(filing)!: rename latest to recent`) or a
`BREAKING CHANGE:` line in the body; it makes the next release major.
