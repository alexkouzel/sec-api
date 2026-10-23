# Conventions

How code in this repository is named, structured and written. Tools enforce what they can
(formatting, lint, public API); this file covers what they can't.

Status: the glossary applies now. The other sections are written at the start of
Phase 5 (Kotlin rewrite), together with the API design; until then the Java code stays
consistent with itself and is not restyled.

## Glossary

EDGAR terms as used in code, docs and tests. Names in code follow this table; don't
abbreviate beyond what it lists. "Current name" is the Java code today; "Kotlin name" is
the target, confirmed at the start of Phase 5.

### Identifiers

| Term | Meaning | Current name | Kotlin name |
|------|---------|--------------|-------------|
| CIK | Central Index Key: the SEC's numeric ID for any entity that files or is filed about — companies, funds, and individual insiders alike. Up to 10 digits; zero-padded to 10 in `data.sec.gov` URLs. | `cik`, `issuerCik`, `rptOwnerCik` | `cik` (always qualified by role when the role is known: `issuerCik`, `ownerCik`) |
| Accession number | Unique ID of one filing, e.g. `0001127602-23-028345`: submitter's CIK, two-digit year, sequence. 20 characters with dashes; dashes removed in archive paths. | `accNo` | `accessionNumber` |
| Ticker | Trading symbol, e.g. `AAPL`. Not unique over time; CIK is the stable key. | `ticker`, `issuerTradingSymbol` | `ticker` |

### Filings

| Term | Meaning | Current name | Kotlin name |
|------|---------|--------------|-------------|
| Filing | One submission to EDGAR, identified by its accession number. Contains one or more documents. | `File`, `FileF345` | `Filing` |
| Filing reference | A pointer to a filing from a list (index, feed, submissions): accession number, form type, date, a CIK. No content. | `FileRef` | `FilingRef` |
| Form type | The kind of filing: `4`, `4/A`, `10-K`, `8-K`, `424B2`, ... `/A` marks an amendment of an earlier filing. Matched exactly; EDGAR's own `type=` filters are prefix matches. | `FileType` | `FormType` |
| Document | One file inside a filing (the ownership XML, an exhibit, ...). | `xmlFilename` | `document` |
| Filed date | Date the filing was accepted for filing (`FILED AS OF DATE`, `filingDate`). | `filedAt` | `filedOn` (a `LocalDate`) |
| Period of report | Date the filing reports on; for Form 4, the transaction date that triggered it. | `periodOfReport` | `periodOfReport` |

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

Note: EDGAR's "fiscal quarter" naming in the current code (`loadFiscalQuarter`) is wrong —
indexes are by calendar quarter. Kotlin name: `quarterlyIndex(year, quarter)`.

## Structure

Written at the start of Phase 5: packages by area (`client`, `company`, `filing`,
`ownership`), public API vs internal, where models, parsers and HTTP code live.

## Kotlin style

Written at the start of Phase 5: data classes and immutability, nullability (absent vs
empty EDGAR values), the sealed error type, KDoc on public API.

## Tests

Applies now (see also `CLAUDE.md`):

- Mirror the main package of the code under test; shared helpers live in `testing/`.
- Offline by default. Tags: `live` (real EDGAR), `known-bug` (fails until fixed), `drift`
  (samples live EDGAR, reports differences from the model).
- Fixtures under `src/test/resources/edgar/<source>/` are frozen; ownership filings are
  named `<accession number>.txt`. Add new fixtures and list them in that folder's README.
- Expected values come from the raw data (independently extracted), not from the code's
  own output.
