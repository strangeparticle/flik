# Authoring Flik (for AI generators)

This is the reference for generating Flik documents. It is derived from what the
`flik` interpreter actually accepts. If you generate only the forms below, your
document will validate and run.

## The one rule

**Write the Markdown release runbook you'd write anyway.** The specific phrases in
this guide are the executable parts; every other line (headings, prose, blockquotes,
blank lines) is ignored. You are not writing "code" — you are writing documentation
whose imperative sentences happen to be machine-recognized. Lean on that.

## Two kinds of document

1. **Entry document** — the program's entry point: prerequisites + the procedure.
2. **Stage document** — one stage of the procedure, reached from a callout. Its
   filename is derived from the callout name (see "Callout → filename").

### Entry document skeleton

````markdown
# Release the Thing

# Pre-requisites
flik version: 0.1

Environment Variables:
* APPLE_ID
* APPLE_TEAM_ID

Shell Commands:
* ./gradlew
* xcrun

# Release Procedure
Run these steps sequentially, in the order shown:
* back up to file `../the-thing-backup.tar.gz`
* [Re-apply release-only changes]
* [Build the artifact]
* restore the repository
````

### Stage document skeleton

A callout `[Build the artifact]` is resolved to the sibling file
`build-the-artifact.flik.md`:

`````markdown
# Build the artifact

flik version: 0.1

## Copy assets into the project

* Copy file from: `./resources/entitlements.plist` to: `app/packaging/entitlements.plist`

## Edit the build file

In file: `app/build.gradle.kts`

Find this section:

```kotlin
signing { sign.set(false) }
```

Replace it with:

```kotlin
signing { sign.set(true) }
```

## Build

Run command:

```shell
./gradlew :app:packageReleaseDmg
```

Expect file to exist: `{{ project_root }}/app/build/release/TheThing.dmg`

## Verify

Run these checks in parallel:
* run `codesign --display --verbose=4 /Volumes/TheThing/TheThing.app` and expect the output to contain `Developer ID Application`
* run `lipo -archs /Volumes/TheThing/TheThing.app/Contents/MacOS/TheThing` and report the architectures
`````

## Vocabulary (everything the interpreter recognizes)

| Form | Document | Effect |
|------|----------|--------|
| `flik version: <x>` | both (first prerequisite) | the Flik version the document targets |
| `Environment Variables:` then `* NAME` bullets | entry | required env vars — checked before anything runs |
| `Shell Commands:` then `* cmd` bullets | entry | required commands on PATH — checked first |
| `Run these steps sequentially, in the order shown:` then bullets | entry | the procedure |
| `back up to file <path>` | entry step | back up the project; the location is remembered |
| `restore the repository` | entry step | restore from the remembered backup |
| `[Callout Name]` | entry step | run the stage file derived from the name |
| ``* Copy file from: `src` to: `dst` `` | stage | copy (`src` relative to the stage doc, `dst` to the project root) |
| ``In file: `path` `` + `Find this section:` / `Replace it with:` blocks | stage | in-place find/replace edit |
| `Run command:` + a fenced ```` ```shell ```` block | stage | run a shell command; non-zero exit fails the run |
| ``Expect file to exist: `path` `` | stage | assert a file exists |
| `Run these checks in parallel:` then `* … run `cmd` … contain `x`` bullets | stage | run each check; assert output contains `x` when stated |
| `{{ project_root }}` | anywhere | the root path of the project Flik operates on |

## The five rules you must follow

1. **Callouts are bare bracketed names** — `* [Build the artifact]`. No link, no path,
   no `.flik.md`. Flik computes the filename.
2. **Fenced code blocks are literal payloads.** What you put in a ```` ```kotlin ````
   / ```` ```shell ```` block is matched or executed verbatim. Never reformat or
   "tidy" them.
3. **Fail-on-error is the default.** A failed step aborts the run. Do **not** write
   error handling, retries, or conditionals — there is no need.
4. **Hardcode values; do not DRY.** There is no variable-declaration syntax. Write
   literal values (e.g. `TheThing`, `1.4.2`) directly, repeated as needed. The only
   interpolation is `{{ project_root }}`.
5. **`flik version:` is the first prerequisite line.**

## Callout → filename normalization

Lowercase → replace every run of non-`[a-z0-9]` with a single `-` → trim `-` →
append `.flik.md`.

| Callout | File |
|---------|------|
| `[Re-apply release-only changes]` | `re-apply-release-only-changes.flik.md` |
| `[Build the signed, notarized DMG]` | `build-the-signed-notarized-dmg.flik.md` |
| `[Check release prerequisites]` | `check-release-prerequisites.flik.md` |

## The generate → validate → fix loop

This is the main reason Flik is easier to generate than a shell script: **check
before you run.**

```sh
flik validate path/to/entry.flik.md   # parses, resolves callouts, no execution
flik run      path/to/entry.flik.md --project-root <dir>
```

Generate a document, run `flik validate`, read the diagnostic, fix, repeat. Only
then `flik run`.

## Implemented now vs. planned

So you don't generate forms that won't run yet:

- **Implemented:** everything in the Vocabulary table above; sequential entry
  procedures; prerequisite checks; backup/restore; `{{ project_root }}`.
- **Planned (do not rely on yet):** top-level parallel step lists (`Run these steps
  in parallel:` at the entry level); loose-whitespace matching for `Find this
  section:` (current matching is exact — make find blocks match the file verbatim);
  declarative overrides like "continue if this fails"; rollback records and
  execution-artifact logs. Parallel *checks* inside a stage are accepted, but
  currently execute sequentially.
