---
name: release
description: Release a new version of the library to GitHub Packages, e.g. "/release 2.1.0". Verifies the build against live EDGAR, publishes, tags and creates a GitHub release.
---

# Release a version

Argument: the version, e.g. `2.1.0` (semantic versioning). If none is given, suggest one from
the latest tag (`git tag --sort=-v:refname | head -1`) and the commits since it: a removed or
changed public API is a major bump, new API is minor, a fix is patch. Confirm it with the user.

## 1. Check the starting point

- On `main`, up to date with `origin/main`, working tree clean (`git status`).
- The tag `v<version>` doesn't exist yet (`git tag -l v<version>`).

## 2. Verify

- `./gradlew check`
- `./gradlew liveTest` (needs env `EDGAR_USER_AGENT`, `Name contact@domain`)

Stop and report if either fails.

## 3. Publish

Publishing can't be undone, so show the user the version and the commits going into it
(`git log --oneline <latest tag>..HEAD`) and wait for their go-ahead. Then:

```bash
./gradlew -Pversion=<version> publish
```

It needs `GPR_USERNAME` and `GPR_TOKEN` (a GitHub token with `write:packages`), as env
variables or as `gpr.username`/`gpr.token` in `~/.gradle/gradle.properties`.

## 4. Tag and announce

```bash
git tag v<version>
git push origin v<version>
gh release create v<version> --generate-notes
```

Report the release URL.
