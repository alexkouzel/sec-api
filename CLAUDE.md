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
- Fixtures in `src/test/resources/edgar/` are frozen (see its README); add new files,
  don't refresh existing ones. Specs and XSDs live in `specs/`.

## Commits

Angular convention: `type(scope): subject`, imperative, lowercase, no trailing period.
Types: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `test`, `chore`.
Example: `fix(client): inflate deflate-encoded responses`.

This file is a placeholder; the full version is written in Phase 6.
