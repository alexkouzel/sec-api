---
name: edgar-change
description: Adapt the library after the daily live EDGAR check fails (an open issue labelled edgar-change, a failing liveTest, or a SecException.Parse from EDGAR). Finds what broke, saves the response as a fixture, and fixes the parser or model test-first.
---

# Adapt to an EDGAR change

The daily workflow `.github/workflows/live.yml` runs `./gradlew liveTest` and opens an issue
labelled `edgar-change` when it fails. Work through these steps in order.

## 1. Find what broke

- Find the open issue and its latest run:
  `gh issue list --label edgar-change --state open` and `gh issue view <number> --comments`.
- Read the failures: `gh run view <run-id> --log-failed`. If `gh` isn't available, run
  `./gradlew liveTest` locally (needs env `EDGAR_USER_AGENT`, `Name contact@domain`).
- Each failure names a URL and what went wrong: a `SecException.Parse` message, or a value the
  live test expected (issuer, owners, transaction date or code) that came back empty.
- If every failure is `SecException.Http`, EDGAR was down. Rerun the check
  (`gh workflow run live.yml`) and stop here unless it fails again.

## 2. Save the response as a fixture

Fixtures in `src/testFixtures/resources/edgar/<source>/` are frozen: add a new file, never edit
one. Fetch the URL from the failure:

```bash
curl -A "$EDGAR_USER_AGENT" --compressed -o src/testFixtures/resources/edgar/<source>/<name> <url>
```

Name ownership filings `<accession number>.txt`. Add a row for the file to
`src/testFixtures/resources/edgar/README.md` saying what changed in it.

## 3. Fix it test-first

- Find the parser for the source: `filing/LatestFeed.kt`, `DailyList.kt`, `QuarterlyIndex.kt`,
  `Submissions.kt`, `company/Tickers.kt`, or `ownership/FilingText.kt` with `ownership/xml/`.
- Write a unit test on the new fixture in the parser's test file (`<Name>Test.kt`). Take the
  expected values from the raw response, not from the code. Run it and see it fail.
- Fix the parser or the model until it passes. Follow docs/CONVENTIONS.md and keep the
  change minimal; existing fixtures must keep passing.
- New ownership spec: if the SEC published one (https://www.sec.gov/submit-filings/technical-specifications),
  put it in `specs/ownership-xml-v<version>/` as `specs/README.md` describes, and update that
  README. `OwnershipDocumentTest` then names every element or transaction code still missing
  from `ownership/xml` and `TransactionCode`.

## 4. Verify and finish

- `./gradlew check` and `./gradlew liveTest` must both pass.
- Commit as `fix(<area>): <what EDGAR changed>`, describing the change in the body.
- Ask the user before releasing (use the `release` skill) and before closing the issue with a
  comment linking the fix.
