# Conventions

How code in this repository is named, structured and written. Tools enforce what they can
(formatting, lint, public API); this file covers what they can't.

## Glossary

EDGAR terms as used in code, docs and tests. Names in code follow this table; don't
abbreviate beyond what it lists.

### Identifiers

| Term | Meaning | Name in code |
|------|---------|--------------|
| CIK | Central Index Key: the SEC's numeric ID for any entity that files or is filed about — companies, funds, and individual insiders alike. Up to 10 digits; zero-padded to 10 in `data.sec.gov` URLs. | `cik`; on a type named after its role (`Issuer.cik`, `ReportingOwner.cik`), or a related CIK (`FilingRef.cik`) |
| Accession number | Unique ID of one filing, e.g. `0001127602-23-028345`: submitter's CIK, two-digit year, sequence. 20 characters with dashes; dashes removed in archive paths. | `accessionNumber` |
| Ticker | Trading symbol, e.g. `AAPL`. Not unique over time; CIK is the stable key. | `ticker` |

### Filings

| Term | Meaning | Name in code |
|------|---------|--------------|
| Filing | One submission to EDGAR, identified by its accession number. Contains one or more documents. | `OwnershipFiling` (Forms 3, 4, 5) |
| Filing reference | A pointer to a filing from a list (index, feed, submissions): accession number, form type, date, a CIK (a related CIK; its role depends on the source). No content. | `FilingRef` |
| Form type | The kind of filing: `4`, `4/A`, `10-K`, `8-K`, `424B2`, ... `/A` marks an amendment of an earlier filing. Matched exactly; EDGAR's own `type=` filters are prefix matches. | `FormType` (keeps the raw value) |
| Document | One file inside a filing (the ownership XML, an exhibit, ...). | `document` |
| Filed date | Date the filing was accepted for filing (`FILED AS OF DATE`, `filingDate`). | `filedOn` (a `LocalDate`) |
| Period of report | Date the filing reports on; for Form 4, the transaction date that triggered it. | `periodOfReport` |

### Roles

A filing relates to several CIKs. Use the role name, never a bare "CIK", when the role is known.

| Role | Meaning | Where it appears |
|------|---------|------------------|
| Issuer | Company whose securities the filing is about. | Ownership XML `<issuer>`; latest feed entries labelled `(Issuer)` |
| Reporting owner | Insider (director, officer, 10% owner, other) reporting their ownership. | Ownership XML `<reportingOwner>`; latest feed `(Reporting)` |
| Filer | Entity the filing is filed under, for forms without issuer/owner roles. | Latest feed `(Filer)` |
| Subject company | Company a filing is about when someone else files (e.g. Schedule 13D/G). | Latest feed `(Subject)` |
| Filed by | The other party on such a filing (the investor filing a Schedule 13D/G). | Latest feed `(Filed by)` |
| Related CIK | A CIK attached to the filing whose role the source doesn't say. | Daily page (`cgi-bin/current`), quarterly `master.idx` |

The latest feed and daily page list one entry per related CIK, so a filing appears several
times; results are deduplicated by accession number. From the latest feed the Issuer's or
Subject's CIK is kept (else the first entry's); from the daily page, the first row's.

### Ownership filings (Forms 3, 4, 5)

| Term | Meaning |
|------|---------|
| Form 3 | Initial statement of ownership when someone becomes an insider. |
| Form 4 | Change in ownership, filed within two business days of a transaction. |
| Form 5 | Annual statement of transactions not reported on Form 4. |
| Non-derivative security | The security itself, e.g. common stock. |
| Derivative security | A right to acquire one: options, warrants, RSUs, convertibles. Has an underlying security, exercise price, exercise and expiration dates. |
| Transaction | A change in holdings on a date, with a transaction code. |
| Holding | A position held without a reported transaction (Form 3, or unchanged indirect holdings). |
| Transaction code | One letter for the kind of transaction (`P` purchase, `S` sale, `A` grant, `M` option exercise, ...); the XSD's `TRANS_CODE_PICKLIST` is the authority. |
| Direct / indirect ownership | `D`: held by the owner; `I`: held through another entity, with the nature of ownership explained. |
| Footnote | Text attached to a value by ID (`F1`, `F2`, ...). A value may carry several footnote IDs, or a footnote instead of a value. |
| Schema version | `schemaVersion` in the XML, e.g. `X0609`; the spec in `specs/` lists them. |

### Sources

| Term | Meaning | Endpoint |
|------|---------|----------|
| Company tickers | All companies with a ticker. | `files/company_tickers_exchange.json` |
| Submissions | A CIK's metadata and filing history. | `data.sec.gov/submissions/CIK##########.json` |
| Latest filings feed | Most recent filings, Atom. | `cgi-bin/browse-edgar?action=getcurrent` |
| Daily list | Filings from N days ago, HTML. | `cgi-bin/current?q1=N` |
| Quarterly index | Every filing in a calendar quarter. | `Archives/edgar/full-index/<year>/QTR<n>/master.idx` |
| Filing text | Full submission: SGML header plus documents. | `Archives/edgar/data/<cik>/<accession>/<accession>.txt` |

Quarterly indexes are by calendar quarter, not fiscal quarter: `Filings.quarter(year, quarter)`.

## Structure

Packages and test layout are in [ARCHITECTURE.md](ARCHITECTURE.md).

## Naming

- Names say what a thing is in the glossary's words: `FilingRef`, `formType`, `accessionNumber`.
- Short and specific. A type is one to three words; a local is usually one (`refs`,
  `filing`, `owner`). The package already gives context: `filing.LatestFeed`, not
  `filing.LatestFilingFeedParser`.
- No role suffixes that say nothing: no `Utils`, `Helper`, `Manager`, `Impl`, `Data`, `Info`.
  A parser is named after what it reads (`LatestFeed.parse(...)`), a facade after its area
  in plural (`Filings`).
- No type names in variable names: `refs`, not `refList` or `fileRefArray`.
- Abbreviations only from the glossary (`cik`); everything else spelled out.
- Booleans read as questions: `isAmendment`, `hasFootnotes`.
- Tests: `FooTest` for `Foo.kt`; methods in backticks as a sentence:
  `` `type filter keeps exact matches only`() ``.

## Comments

Code should explain itself through names and structure; a comment is a last resort.

- Write one only for what the code can't say: an EDGAR quirk, a reason, a non-obvious
  constraint. `// EDGAR's type filter is a prefix match: "4" also returns "424B2".`
- Never restate the code, label sections, or keep commented-out code.
- KDoc only on public API, and only where name and signature leave a question:
  units, request cost, EDGAR behaviour. A function named `latest(count, form)` needs none.
- No file headers, no `@author`, no `@param` that repeats the parameter's name.

## Kotlin style

- Formatting by ktlint, lint by detekt, public API by explicit API mode. Rules in tools
  where possible; this file holds only what they can't check.
- Data classes of `val`s; `List`/`Map` (read-only) in signatures. No `lateinit`, no `!!`.
- Nullability means absence: a field EDGAR leaves out or empty is `null`. Collections are
  never null; missing means empty.
- Errors: throw `SecException.Http` or `SecException.Parse` (unchecked); never return
  `null` for a failure. Messages name the URL and what was expected.
- Don't pack code onto one line. Give intermediate values a name instead of chaining many
  calls or nesting expressions; a plain `if` or loop over a clever one-liner. Lines are at most
  100 characters (ktlint and detekt enforce it).
- Guard with `?: throw` and early `return` where it keeps the main path flat.
- One public top-level type per file, named after the file; small related types may share
  a file named after their concept (see ARCHITECTURE.md).
- Public API stays usable from Java: no value classes in signatures, `@JvmOverloads` on
  functions with default arguments, `@JvmField`/`@JvmStatic` for constants.
  `src/test/java/.../JavaApiTest` uses the API from Java, so a change that breaks that fails
  `check`.

## Tests

- Layout and tasks: see ARCHITECTURE.md.
- Offline by default; live EDGAR only in `liveTest`.
- Fixtures under `src/test/resources/edgar/<source>/` are frozen; ownership filings are
  named `<accession number>.txt`. Add new fixtures and list them in that folder's README.
- Expected values come from the raw data (independently extracted), not from the code's
  own output.
- One behaviour per test, named as a sentence; arrange, act, assert separated by a blank
  line, no comments needed.
