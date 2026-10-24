---
name: release
description: Release the next version of the library ("/release"). Works out the version from the commits since the last release, verifies against live EDGAR, tags it, creates a GitHub release and checks that JitPack builds it.
---

# Release the next version

## 1. Check the starting point

- On `main`, up to date with `origin/main`, working tree clean (`git status`).
- There are commits since the last release; if not, say so and stop.

## 2. Work out the version

Versions are semantic (`MAJOR.MINOR.PATCH`) and tagged `v<version>`.

1. The last release is the highest tag: `git tag --list 'v*' --sort=-v:refname | head -1`.
2. List the commits since it: `git log --format='%H%n%s%n%b%n---' <tag>..HEAD`. Commit
   subjects follow the Angular convention (`type(scope): subject`, see CLAUDE.md).
3. Bump the last version by the largest change among them:
   - **major** if any commit marks a breaking change: `!` before the colon (`feat!:`,
     `refactor(http)!:`) or a `BREAKING CHANGE:` line in the body;
   - **minor** if any commit is a `feat`;
   - **patch** otherwise (`fix`, `perf`, `refactor`, ...).
   Bumping resets the lower parts: `2.3.1` → major `3.0.0`, minor `2.4.0`, patch `2.3.2`.
4. Only `build`, `ci`, `docs`, `test` or `chore` commits change nothing users get: say so and
   ask whether to release anyway (as a patch).

## 3. Verify

- `./gradlew check`
- `./gradlew liveTest` (needs env `EDGAR_USER_AGENT`, `Name contact@domain`)

Stop and report if either fails.

## 4. Release

A release can't be taken back once someone depends on it. Show the user the version, why (the
commit that decided the bump) and the commits going in, and wait for their go-ahead. Then:

```bash
git tag v<version>
git push origin v<version>
gh release create v<version> --generate-notes
```

## 5. Check that JitPack builds it

JitPack builds the release from its tag (see `jitpack.yml`) when it is first requested.
Request it, which starts the build, and wait for it, retrying for up to 10 minutes:

```bash
curl -sf https://jitpack.io/com/github/alexkouzel/sec-api/v<version>/sec-api-v<version>.pom
```

If it doesn't appear, read the build log at
`https://jitpack.io/com/github/alexkouzel/sec-api/v<version>/build.log`, report what failed and
fix it in a patch release.

Report the GitHub release URL and the dependency:
`implementation("com.github.alexkouzel:sec-api:v<version>")` from `maven("https://jitpack.io")`.
