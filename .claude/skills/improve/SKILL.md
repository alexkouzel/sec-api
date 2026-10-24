---
name: improve
description: Review the library's code, structure and docs, make the changes worth making, each checked by an independent reviewer agent, and open one pull request with them. Use when asked to improve, polish, simplify or tidy the project.
---

# Improve the project

Run to the end without asking the user anything; the pull request is the result. If nothing is
worth changing, say so and open none: no change is better than a change for its own sake.

## What counts as an improvement

Read CLAUDE.md and the files in docs/ first; their rules apply. On top:

- **Simpler.** Fewer files, types, lines and dependencies for the same behaviour. Deleting
  beats adding; a new abstraction, setting or layer has to remove more than it adds.
- **Readable.** Code reads top to bottom: named values, no packed lines, steps separated by
  blank lines, names a reader understands without the comment.
- **Consistent.** The same job done the same way in every area, with the glossary's names.
- **Docs that are true, said once, in the right file:** the README for users, CLAUDE.md for
  commands and managing the project, ARCHITECTURE.md for layout and design, CONVENTIONS.md for
  rules, GLOSSARY.md for EDGAR's terms, specs/README.md for its sources' URLs and formats.

Not improvements: renames for taste, reformatting that ktlint accepts, comments that restate the
code, features nobody asked for, and handling for input EDGAR doesn't produce.

### Declined

Ideas the user turned down. Don't propose them again; add to this list when the user declines
another one.

- Renaming the repository; it stays `sec-api`.
- Abbreviating `accessionNumber` (e.g. `accNo`).
- Release automation or a release skill; releases are made by hand.
- detekt, a version catalog, or other build tooling besides ktlint.

## Steps

1. Branch from the latest `main`.
2. Read all of `src/main`, the tests' structure, the build, the workflows and the docs. List the
   candidate changes, each with what it changes and why a reader or maintainer is better off.
   Rank them by that value and drop the ones the section above rules out.
3. For each candidate, best first, up to five per run:
   1. Make the change. A change in behaviour starts with a failing test; fixtures stay frozen.
      `./gradlew check` must pass.
   2. Have it reviewed: start a new general-purpose agent with the reviewer prompt below. It
      sees only the repository and the uncommitted diff, never your reasons for the change.
   3. Fix what the reviewer finds and send it back to the same reviewer, for up to three rounds.
      If it still doesn't approve, discard the change (`git checkout -- .`, and remove new files)
      and note it for the pull request.
   4. Commit the approved change on its own, as CLAUDE.md's Commits section says.
4. With at least one commit, push and open a pull request. Its description lists each commit in
   one line, then the changes the reviewer rejected and why. Without one, report that nothing
   was worth changing.

## Reviewer prompt

> You review one change to sec-api, a small Kotlin library for SEC EDGAR. You didn't write it
> and don't know why it was made: judge it only from the change and the repository. Read
> CLAUDE.md, docs/CONVENTIONS.md, docs/GLOSSARY.md and the section "What counts as an
> improvement" in .claude/skills/improve/SKILL.md, then the change (`git diff` and `git status`
> for new files).
>
> Check that it is correct (behaviour unchanged unless it means to change it, and then tested),
> that the result is simpler or clearer than before and not just different, that it follows the
> conventions, and that it is worth its diff. Don't edit anything.
>
> Answer APPROVE, or list the problems, each with file:line and what to do instead.
