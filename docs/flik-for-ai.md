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

## Comments

An **indented sub-bullet** is a comment — a note for human readers that the
interpreter ignores. Useful for explaining a prerequisite:

```markdown
* shell command: `./gradlew`
  * provided by the flik project — no need to install it
* jdk 17
  * required only to build the binary, not to run it
```

## Vocabulary (everything the interpreter recognizes)

| Form | Kind | Effect |
|------|------|--------|
| ``Authored for compatibility with Flik `v<x>` `` (in the footer) | footer | the Flik version the page was authored for — full or partial semver (`v1`, `v1.0.0`) |
| ``* environment variable: `NAME` `` (or `Environment Variables:` + `* NAME` bullets) | prerequisite | required env vars — checked when the page is entered |
| ``* shell command: `cmd` `` (or `Shell Commands:` + `* cmd` bullets) | prerequisite | required commands on PATH — checked on entry |
| `[Page Name]` | page invocation | run the page whose filename is derived from the name |
| ``Copy file from: `src` to: `dst` `` | command | copy (`src` relative to *this* page, `dst` to the project root) |
| `Run command:` + a fenced ```` ```shell ```` block, or ``* run command: `cmd` `` | command | run a shell command; non-zero exit fails the run |
| ``* capture output as: `NAME` `` (directly after a run command) | modifier | bind that command's (trimmed) output to ``${{ capture.NAME }}`` |
| ``* expect to see: `text` `` (directly after a run command) | modifier | assert that command's output contains `text` (repeatable) |
| `If the command fails:` + a fenced ```` ```shell ```` block (directly after a run command) | modifier | run this fallback if the command exits non-zero, instead of failing |
| ``Expect file to exist: `path` `` | command | assert a file exists |
| ``Expect file to be empty: `path` `` | command | assert `path` is empty — absent, zero bytes, or whitespace-only all pass; otherwise fail, showing the file's current contents (first 10 lines). A guard against shared-resource conflicts |
| ``In file: `path` `` + `Find this section:` / `Replace it with:` blocks | command | in-place find/replace edit |
| ``Write to file: `path` `` + a fenced block | command | write the block's content to `path`, overwriting; creates the file and any missing parent dirs |
| ``Append to file: `path` `` + a fenced block | command | append the block's content to an **existing** `path`; errors if the file does not exist |
| ``Create or append to file: `path` `` + a fenced block | command | append when `path` exists, else create it (and any missing parent dirs) with the block's content |
| `Run these checks in parallel:` then `* … run `cmd` … contain `x`` bullets | command | run each check; assert output contains `x` when stated |
| `back up to file <name>` | command | back up the project to `executions/<run>/backups/<name>` |
| `restore the repository` | command | restore the most recent backup *this page* took |
| `restore from backup <name>` | command | restore a specific named backup |
| `If <predicate>:` + one element, optional `Otherwise:` + one element | command | run the first element when the predicate holds, else the `Otherwise` element (see Branching) |
| ``Depending on `<selector>`:`` + `` * `literal`: <element> `` bullets, optional `* otherwise: <element>` | command | run the branch whose literal equals the interpolated selector, else `otherwise` (see Branching) |
| `${{ project_root }}` | interpolation | the root path of the project Flik operates on |
| `${{ capture.NAME }}` | interpolation | a value bound by an earlier `capture output as: NAME` |
| `${{ env.NAME }}` | interpolation | the value of the `NAME` environment variable, usable anywhere (paths, run/edit blocks); unset is reported by preflight and fails the run if reached |

One-line forms (`Copy file…`, `Expect…`, `back up…`, `restore…`, `[Page]`, and the
prerequisite bullets) may be written as bare lines or as `*` bullets — both work.

## Writing files

`Write to file:`, `Append to file:`, and `Create or append to file:` each take a
backtick `path` followed by a fenced block whose body is the file content. They are
the declarative alternative to a `cat > file <<'EOF'` heredoc in a `Run command:`.

Unlike the verbatim fenced blocks in rule 2, **these blocks are interpolated**:
`${{ env.NAME }}`, `${{ capture.NAME }}`, and `${{ project_root }}` expand inside the
body just as they do in a shell block. The path resolves against the project root
(absolute paths pass through). Use `Write to file:` to (over)write, `Append to file:`
to add to a file that must already exist, and `Create or append to file:` when either
case is fine.

````markdown
Write to file: `${{ project_root }}/build/config/service.env`

```shell
SERVICE_NAME=${{ capture.SERVICE_NAME }}
DEPLOY_REGION=${{ env.DEPLOY_REGION }}
```
````

## Run command modifiers

`capture output as`, `expect to see`, and `If the command fails:` are **modifiers of a
run command** — they operate on *that command's* result and must directly follow it
(blank lines between are fine). They are not standalone steps; there is no "previous
command" reaching backward. One command can carry several:

````markdown
Run command:

```shell
cat VERSION
```

* expect to see: `1.`
* capture output as: `APP_VERSION`
````

`expect to see` and `capture output as` see whichever command actually ran — including
the fallback, when `If the command fails:` is used.

## Naming captures and env vars

Names are documentation. Write the readable runbook you'd write anyway — a capture or
env var name should read naturally in prose and be self-explanatory.

- **UPPERCASE the name**, matching the `${{ env.NAME }}` convention. A capture is just
  another named value, so `* capture output as: \`IMAGE_TAG\`` → `${{ capture.IMAGE_TAG }}`,
  not `image_tag`. Uppercase makes interpolations stand out from the surrounding prose
  and the shell `$VAR`s in your blocks.
- **Spell out full words; don't abbreviate.** `DEPLOYMENT_REPO`, not `DEPLOY_REPO`;
  `SERVICE_NAME`, not `SVC_NAME`; `OPERATING_SYSTEM`, not `OS`. The name is read far more
  often than it's typed, and the runbook should explain itself to a human skimming it.

```markdown
* capture output as: `IMAGE_TAG`
```

…later referenced as `${{ capture.IMAGE_TAG }}` — reads as a sentence, no decoding needed.

## Branching: conditionals and switches

When a procedure forks, write the fork as documentation and let Flik decide the branch.

**Conditional** — `If <predicate>:` then one element, with an optional `Otherwise:` and one
element:

````markdown
If the command `xcodebuild` is available:

Run command:

```shell
./build-with-xcode.sh
```

Otherwise:

* [Build with the command-line tools]
````

**Switch** — `Depending on \`<selector>\`:` then a bullet per value; the selector is an
interpolated value, matched (exact, trimmed) against the backtick literals:

```markdown
Depending on `${{ capture.OPERATING_SYSTEM }}`:

* `Darwin`: [Do the macOS step]
* `Linux`: [Do the Linux step]
* otherwise: [Do the generic step]
```

Rules:

- **A predicate is one of a fixed set** — Flik must decide it deterministically, so only:
  `` the command `X` is available ``, `` the file `X` exists ``, and `` `<value>` is `<literal>` ``
  (the value interpolated, compared for equality). No `and`/`or`/`not`, no expressions.
- **A branch is exactly one element** — a command or a `[Page]` invocation. For a multi-step
  branch, make it a page and invoke it; Flik links and preflights that page like any other.
- **Defaults**: a switch with no matching literal and no `* otherwise:` **fails clearly**; an
  `If` with no `Otherwise:` is a no-op when the predicate is false.
- This is distinct from the inline `If the command fails:` modifier on a single run command —
  that stays the shorthand for "this one command, with a fallback."

## The five rules you must follow

1. **Page invocations are bare bracketed names** — `* [Build the artifact]`. No link,
   no path, no `.flik.md`. Flik computes the filename.
2. **Fenced code blocks are literal payloads.** What you put in a ```` ```kotlin ````
   / ```` ```shell ```` block is matched or executed verbatim. Never reformat them.
3. **Fail-on-error is the default.** A failed element aborts the run. Do **not** write
   error handling — there is no need.
4. **Hardcode values; do not DRY.** There is no variable-declaration syntax. Write
   literal values (e.g. `TheThing`, `1.4.2`) directly, repeated as needed. The only
   interpolations are `${{ project_root }}`, `${{ capture.NAME }}`, and
   `${{ env.NAME }}` — note the **double brace** (GitHub Actions style), which keeps
   them from colliding with the plain shell/Kotlin `$VAR` and `${VAR}` that live in
   your `Run command:` and edit blocks (those pass through untouched). An env var you
   reference only through shell `$VAR` still needs an explicit
   `* environment variable:` prerequisite; a `${{ env.NAME }}` reference is detected
   for you.
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
before you run.** `validate` links the *whole* page graph — every reachable page is
parsed and every callout resolved — and reports **all** structural problems at once
(missing pages, parse errors, cycles, missing version footers), each tagged with its
page.

```sh
flik validate path/to/root.flik.md            # deep-link the whole graph; no execution
flik explain  path/to/root.flik.md            # print the page graph + aggregated prereqs
flik run      path/to/root.flik.md --project-root <dir>
```

`run` additionally **preflights**: it checks every prerequisite across every page up
front and reports all the missing ones together, before doing any work. Generate,
`validate`, read the diagnostics, fix, repeat; then `run`.

All three accept a `.flik.md` page **or a folder** — a folder resolves to its
`<folder-name>.flik.md` root. So put the root page in a folder named after it
(`release-macos-dmg/release-macos-dmg.flik.md`) and callers can just point at the
folder.

## Implemented now vs. planned

So you don't generate forms that won't run yet:

- **Implemented:** everything in the Vocabulary table above; pages that invoke pages
  recursively; per-page prerequisites; the version footer; document-order execution;
  backup/restore; `${{ project_root }}` and `${{ capture.NAME }}`; block-level
  conditionals (`If … Otherwise`) and switches (`Depending on …`).
- **Planned (do not rely on yet):** parallel execution (`Run these checks in parallel:`
  is accepted but currently runs sequentially; parallel *page* invocation isn't in
  yet); loops over invocations; loose-whitespace matching for `Find this section:`
  (matching is currently exact — make find blocks match the file verbatim);
  preflight tagging of branch-only prerequisites as conditional (today every reachable
  page's prerequisites are reported as required, even branches you won't take).
