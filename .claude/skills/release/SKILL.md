---
name: release
description: Release the next version of the library to GitHub Packages ("/release"). Works out the version from the commits since the last release, verifies against live EDGAR, publishes, tags and creates a GitHub release.
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
5. No tag yet: this is the first release of the Kotlin library, which replaces the Java API
   entirely, so it is a major release. Find the highest version already published:
   `gh api /users/alexkouzel/packages/maven/com.alexkouzel.sec-api/versions --jq '.[].name'`,
   and use the next major above it (`1.4.2` → `2.0.0`). If nothing was published, use `1.0.0`;
   if the command fails, ask the user for the last published version.

## 3. Verify

- `./gradlew check`
- `./gradlew liveTest` (needs env `EDGAR_USER_AGENT`, `Name contact@domain`)

Stop and report if either fails.

## 4. Publish

Publishing can't be undone. Show the user the version, why (the commit that decided the bump)
and the commits going in, and wait for their go-ahead. Then:

```bash
./gradlew -Pversion=<version> publish
```

It needs `GPR_USERNAME` and `GPR_TOKEN` (a GitHub token with `write:packages`), as env
variables or as `gpr.username`/`gpr.token` in `~/.gradle/gradle.properties`.

## 5. Tag and announce

```bash
git tag v<version>
git push origin v<version>
gh release create v<version> --generate-notes
```

Report the release URL.
