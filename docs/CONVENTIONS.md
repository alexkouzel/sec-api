# Conventions

How code in this repository is named, structured and written. Tools enforce what they can
(formatting, lint, public API); this file covers what they can't.

## Structure

Packages and test layout are in [ARCHITECTURE.md](ARCHITECTURE.md).

## Naming

- Names say what a thing is in the words of [GLOSSARY.md](GLOSSARY.md): `FilingRef`,
  `formType`, `accessionNumber`.
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
  units, request cost, EDGAR behaviour. A function named `latest(count, formType)` needs none.
- No file headers, no `@author`, no `@param` that repeats the parameter's name.

## Kotlin style

- Formatting by ktlint, public API by explicit API mode. Rules in tools
  where possible; this file holds only what they can't check.
- Data classes of `val`s; `List`/`Map` (read-only) in signatures. No `lateinit`, no `!!`.
- Nullability means absence: a field EDGAR leaves out or empty is `null`. Collections are
  never null; missing means empty.
- Errors: throw `SecException.Http` or `SecException.Parse` (unchecked); never return
  `null` for a failure. Messages name the URL and what was expected.
- Don't pack code onto one line. Give intermediate values a name instead of chaining many
  calls or nesting expressions; a plain `if` or loop over a clever one-liner. Lines are at most
  100 characters (ktlint enforces it).
- Guard with `?: throw` and early `return` where it keeps the main path flat.
- A function that reads a model from a source goes in steps, separated by blank lines: read
  the raw parts, find and check the values, then build the model with named arguments.
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
