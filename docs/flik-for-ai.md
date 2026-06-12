# Authoring Flik (for AI generators)

This is the reference for generating Flik documents. It is derived from what the
`flik` interpreter actually accepts. If you generate only the forms below, your
document will validate and run.

## The one rule

**Write the Markdown release runbook you'd write anyway.** The specific phrases in
this guide are the executable parts; every other line (headings, prose, blockquotes,
blank lines) is ignored. You are not writing "code" — you are writing documentation
whose imperative sentences happen to be machine-recognized. Lean on that.

## Everything is a Page

A Flik file is a **Page**. There is no special "entry" vs "sub" document — the page
you point `flik run` at (the "root") is just where execution starts; structurally
it's identical to every page it calls. A page has:

- a `# Title`,
- optional **prerequisites** (env vars and shell commands) at the top,
- a **body**: a sequence of elements executed in **document order**, and
- a **footer** declaring the Flik version it was authored for.

A body element is one of exactly two things: a **Command** (a built-in) or a **Page
invocation** (a callout to another page).

> **Pages must be followable without Flik.** A human reading the page, or an AI
> following it, should be able to do the procedure by hand. So **Flik is never a
> prerequisite**, and a page contains **no "run this with `flik`" instructions** — the
> Flik version lives only in the footer, as an authoring note.

### Page skeleton

`````markdown
# Build the artifact

# Pre-requisites
* environment variable: `APPLE_ID`
* shell command: `./gradlew`

# Body

* back up to file `pre-build-backup.tar.gz`
* [Re-apply release-only changes]

In file: `app/build.gradle.kts`

Find this section:

```kotlin
signing { sign.set(false) }
```

Replace it with:

```kotlin
signing { sign.set(true) }
```

Run command:

```shell
./gradlew :app:packageReleaseDmg
```

Expect file to exist: `${{ project_root }}/app/build/release/TheThing.dmg`

* restore the repository

---

_Authored for compatibility with Flik `v0.1`._
`````

Elements run top to bottom in the order they appear. **You do not write a "run these
in order" directive** — document order *is* the order.

## Prerequisites

Each prerequisite is a `* <kind>: \`value\`` bullet. When there are **several** of one
kind, you may instead group them under a plural label + bullet list. Both parse.

```markdown
# Pre-requisites
* shell command: `./gradlew`         # one of a kind → inline

Environment Variables:               # several of a kind → grouped label + list
* APPLE_ID
* APPLE_TEAM_ID
```

## Vocabulary (everything the interpreter recognizes)

| Form | Kind | Effect |
|------|------|--------|
| ``Authored for compatibility with Flik `v<x>` `` (in the footer) | footer | the Flik version the page was authored for — full or partial semver (`v1`, `v1.0.0`) |
| ``* environment variable: `NAME` `` (or `Environment Variables:` + `* NAME` bullets) | prerequisite | required env vars — checked when the page is entered |
| ``* shell command: `cmd` `` (or `Shell Commands:` + `* cmd` bullets) | prerequisite | required commands on PATH — checked on entry |
| `[Page Name]` | page invocation | run the page whose filename is derived from the name |
| ``Copy file from: `src` to: `dst` `` | command | copy (`src` relative to *this* page, `dst` to the project root) |
| `Run command:` + a fenced ```` ```shell ```` block, or ``* run command: `cmd` `` | command | run a shell command; non-zero exit fails the run; its output is remembered |
| ``* expect to see `text` `` | command | assert the previous command's output contains `text` |
| ``Expect file to exist: `path` `` | command | assert a file exists |
| ``In file: `path` `` + `Find this section:` / `Replace it with:` blocks | command | in-place find/replace edit |
| `Run these checks in parallel:` then `* … run `cmd` … contain `x`` bullets | command | run each check; assert output contains `x` when stated |
| `back up to file <name>` | command | back up the project to `executions/<run>/backups/<name>` |
| `restore the repository` | command | restore the most recent backup *this page* took |
| `restore from backup <name>` | command | restore a specific named backup |
| `${{ project_root }}` | interpolation | the root path of the project Flik operates on |

One-line forms (`Copy file…`, `Expect…`, `back up…`, `restore…`, `[Page]`, and the
prerequisite bullets) may be written as bare lines or as `*` bullets — both work.

## The five rules you must follow

1. **Page invocations are bare bracketed names** — `* [Build the artifact]`. No link,
   no path, no `.flik.md`. Flik computes the filename.
2. **Fenced code blocks are literal payloads.** What you put in a ```` ```kotlin ````
   / ```` ```shell ```` block is matched or executed verbatim. Never reformat them.
3. **Fail-on-error is the default.** A failed element aborts the run. Do **not** write
   error handling — there is no need.
4. **Hardcode values; do not DRY.** There is no variable-declaration syntax. Write
   literal values (e.g. `TheThing`, `1.4.2`) directly, repeated as needed. The only
   interpolation is `${{ project_root }}` — note the **double brace** (GitHub Actions
   style), which keeps it from colliding with the plain shell/Kotlin `$VAR` and
   `${VAR}` that live in your `Run command:` and edit blocks (those pass through
   untouched).
5. **Declare the Flik version in a footer, never as a prerequisite** — a final line
   `_Authored for compatibility with Flik `v0.1`._` after a `---`. Pages carry no
   `flik run` instructions; they must be followable without Flik.

## Bullets: when to use them

A bullet list is the natural way to **group short, one-line invocations** —
especially page invocations (`* [Page A]` / `* [Page B]`) and short commands
(`* back up to file x`). Block-carrying commands (`Run command:`, `In file:`) don't
go in bullets; they sit in document flow with their fenced blocks. Either way the
page runs every recognized element in document order.

## Page name → filename normalization

Lowercase → replace every run of non-`[a-z0-9]` with a single `-` → trim `-` →
append `.flik.md`.

| Invocation | File |
|---|---|
| `[Re-apply release-only changes]` | `re-apply-release-only-changes.flik.md` |
| `[Build the signed, notarized DMG]` | `build-the-signed-notarized-dmg.flik.md` |

## The generate → validate → fix loop

This is the main reason Flik is easier to generate than a shell script: **check
before you run.**

```sh
flik validate path/to/page.flik.md   # parses, resolves invocations, no execution
flik run      path/to/page.flik.md --project-root <dir>
```

Generate a page, run `flik validate`, read the diagnostic, fix, repeat. Only then
`flik run`.

## Implemented now vs. planned

So you don't generate forms that won't run yet:

- **Implemented:** everything in the Vocabulary table above; pages that invoke pages
  recursively; per-page prerequisites; the version footer; document-order execution;
  backup/restore; `${{ project_root }}`.
- **Planned (do not rely on yet):** parallel execution (`Run these checks in parallel:`
  is accepted but currently runs sequentially; parallel *page* invocation isn't in
  yet); loops and conditionals over invocations; loose-whitespace matching for `Find
  this section:` (matching is currently exact — make find blocks match the file
  verbatim).
