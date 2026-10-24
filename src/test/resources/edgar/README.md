# EDGAR fixtures

Raw responses captured from live EDGAR on 2026-10-23 (see `specs/README.md` for each source).
Frozen on purpose: tests assert exact values, so do not refresh in place — add new files instead.

| Path | Source | Trimmed |
|------|--------|---------|
| `files/company_tickers_exchange.json` | company tickers | first 25 rows |
| `submissions/CIK0000946581.json` | submissions JSON | first 40 `recent` filings; includes `isXBRLNumeric` |
| `submissions/CIK0000946581-submissions-001.json` | older filings for the same CIK (same shape as `filings.recent`) | first 30 of 1,105 |
| `latest/getcurrent-count-10.atom` | latest feed, no type | no |
| `latest/getcurrent-count-40-type-4.atom` | latest feed, `type=4` (prefix filter: mostly 424B2/497) | no |
| `latest/getcurrent-count-20-type-SCHEDULE13.atom` | latest feed, `type=SCHEDULE 13`: `(Subject)` and `(Filed by)` entry pairs, in either order | no |
| `current/current-q1-1.html` | daily page, 1 day ago | first 60 rows |
| `current/current-q1-1-q3-4.html` | daily page, 1 day ago, `q3=4` | first 60 rows |
| `full-index/master-2022-QTR3.idx` | quarterly index | header + 200 rows |
| `full-index/master-2026-QTR4.idx` | quarterly index | header + 200 rows |
| `ownership/<accession number>.txt` | filing text | no |

Ownership filings:

| Accession | Form | Schema | Why |
|-----------|------|--------|-----|
| 0001062993-22-016446 | 3 | X0206 | 2022 baseline |
| 0001000045-22-000004 | 4 | X0306 | 2022 baseline |
| 0001437749-22-020350 | 5 | X0306 | 2022 baseline |
| 0001437749-22-017271 | 5/A | X0306 | only 5/A sample (none in 2026 Q4 so far) |
| 0001127602-23-028345 | 4 | X0508 | a sale between indirect holdings; the root README's example |
| 0000921895-26-002730 | 3 | X0607 | 10 reporting owners, 12 holdings, footnotes, `rptOwnerCountry` |
| 0000929638-26-003820 | 3 | X0607 | no holdings |
| 0001193125-26-427843 | 3/A | X0607 | amendment, 3 holdings |
| 0001315098-26-000186 | 4 | X0609 | 4 transactions, 3 holdings, 8 footnotes |
| 0001701247-26-000014 | 4 | X0609 | derivative and non-derivative transactions |
| 0001493152-26-046274 | 4 | X0609 | non-US owner address |
| 0001887616-26-000024 | 4/A | X0609 | amendment, 6 + 3 transactions |
| 0001225208-26-008370 | 5 | X0609 | 12 transactions |
| 0001062993-26-005298 | 5 | X0609 | holding, non-US owner address |
