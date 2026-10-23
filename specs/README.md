# Specs

Reference material for the EDGAR sources this library reads. Checked 2026-10-23.

## Ownership filings (Forms 3, 4, 5)

`ownership-xml-v5.5/` — EDGAR Ownership XML Technical Specification, version 5.5 (March 2026).

- `ownership-xml-technical-specification.pdf` — the specification.
- `schemas/` — the "reduced content" XSDs from the same package. Their header comments
  still say `X0306`, but the content defines the 2026 elements
  (`issuerForeignTradingSymbol`, `rptOwnerNonUSAddressFlag`, `rptOwnerNonUSStateTerritory`,
  `rptOwnerCountry`).
- Filings seen in October 2026 carry `schemaVersion` `X0607` (Form 3) and `X0609` (Forms 4, 5);
  2022 filings carry `X0206` (Form 3) and `X0306` (Forms 4, 5); 2023 Form 4 carries `X0508`.
- Source: https://www.sec.gov/submit-filings/technical-specifications
  (download: https://www.sec.gov/files/edgar/filer-information/specifications/ownershipxmltechspec-v5-5.zip)

## Other sources (no formal spec)

| Source | URL | Notes |
|--------|-----|-------|
| Company tickers | `https://www.sec.gov/files/company_tickers_exchange.json` | `{fields: [cik, name, ticker, exchange], data: [[...]]}` |
| Submissions | `https://data.sec.gov/submissions/CIK##########.json` | Columnar `filings.recent` arrays; older filings in `filings.files`. Fields get added (e.g. `isXBRLNumeric`). |
| Latest filings | `https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&output=atom` | Atom. `type` is a **prefix** filter. One entry per related CIK (Filer/Issuer/Reporting). |
| Daily filings | `https://www.sec.gov/cgi-bin/current?q1=<daysAgo>&q3=<type>` | HTML `<pre>` between two `<hr>`. Same prefix filter and per-CIK duplicates. |
| Quarterly index | `https://www.sec.gov/Archives/edgar/full-index/<year>/QTR<n>/master.idx` | 11 header lines, then `CIK\|Company Name\|Form Type\|Date Filed\|Filename`. |
| Filing text | `https://www.sec.gov/Archives/edgar/data/<cik>/<accNoNoDashes>/<accNo>.txt` | SGML header, then documents; ownership XML inside `<XML>`. |

Official pages:

- API overview: https://www.sec.gov/search-filings/edgar-application-programming-interfaces
- Fair access (User-Agent format, 10 requests/second): https://www.sec.gov/about/developer-resources
- Accessing EDGAR data (indexes, archives): https://www.sec.gov/search-filings/edgar-search-assistance/accessing-edgar-data

Fixtures captured from these sources live in `src/test/resources/edgar/`.
