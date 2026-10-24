# Glossary

EDGAR terms as used in code, docs and tests. Names in code follow the tables; don't
abbreviate beyond what they list. Code rules are in [CONVENTIONS.md](CONVENTIONS.md).

## Identifiers

| Term | Meaning | Name in code |
|------|---------|--------------|
| CIK | Central Index Key: the SEC's numeric ID for any entity that files or is filed about — companies, funds, and individual insiders alike. Up to 10 digits; zero-padded to 10 in `data.sec.gov` URLs. | `cik`; on a type named after its role (`Issuer.cik`, `ReportingOwner.cik`), or a related CIK (`FilingRef.cik`) |
| Accession number | Unique ID of one filing, e.g. `0001127602-23-028345`: submitter's CIK, two-digit year, sequence. 20 characters with dashes; dashes removed in archive paths. | `accessionNumber` |
| Ticker | Trading symbol, e.g. `AAPL`. Not unique over time; CIK is the stable key. | `ticker` |

## Filings

| Term | Meaning | Name in code |
|------|---------|--------------|
| Filing | One submission to EDGAR, identified by its accession number. Contains one or more documents. | `OwnershipFiling` (Forms 3, 4, 5) |
| Filing reference | A pointer to a filing from a list (index, feed, submissions): accession number, form type, date, a CIK (a related CIK; its role depends on the source). No content. | `FilingRef` |
| Form type | The kind of filing: `4`, `4/A`, `10-K`, `8-K`, `424B2`, ... `/A` marks an amendment of an earlier filing. Matched exactly; EDGAR's own `type=` filters are prefix matches. | `FormType` (keeps the raw value) |
| Document | One file inside a filing (the ownership XML, an exhibit, ...). | `document` |
| Filed date | Date the filing was accepted for filing (`FILED AS OF DATE`, `filingDate`). | `filedOn` (a `LocalDate`) |
| Period of report | Date the filing reports on; for Form 4, the transaction date that triggered it. | `periodOfReport` |

## Roles

A filing relates to several CIKs. Use the role name, never a bare "CIK", when the role is known.

| Role | Meaning | Where it appears |
|------|---------|------------------|
| Issuer | Company whose securities the filing is about. | Ownership XML `<issuer>`; latest feed entries labelled `(Issuer)` |
| Reporting owner | Insider (director, officer, 10% owner, other) reporting their ownership. | Ownership XML `<reportingOwner>`; latest feed `(Reporting)` |
| Filer | Entity the filing is filed under, for forms without issuer/owner roles. | Latest feed `(Filer)` |
| Subject company | Company a filing is about when someone else files (e.g. Schedule 13D/G). | Latest feed `(Subject)` |
| Filed by | The other party on such a filing (the investor filing a Schedule 13D/G). | Latest feed `(Filed by)` |
| Related CIK | A CIK attached to the filing whose role the source doesn't say. | Daily list, quarterly index |

The latest feed, daily list and quarterly index list one entry per related CIK, so a filing
appears several times; results are deduplicated by accession number. From the latest feed the
Issuer's or Subject's CIK is kept (else the first entry's); from the daily list and quarterly
index, the first row's.

## Ownership filings (Forms 3, 4, 5)

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

## Sources

| Term | Meaning | Name in code |
|------|---------|--------------|
| Company tickers | All companies with a ticker. | `Tickers` |
| Submissions | A CIK's metadata and filing history. | `Submissions` |
| Latest filings feed | The most recent filings. | `LatestFeed` |
| Daily list | The filings of one day, N days ago. | `DailyList` |
| Quarterly index | Every filing in a calendar quarter. | `QuarterlyIndex` |
| Filing text | A full submission: SGML header plus documents. | `FilingText` |

URLs and formats are in [specs/README.md](../specs/README.md).

Quarterly indexes are by calendar quarter, not fiscal quarter: `Filings.quarter(year, quarter)`.
