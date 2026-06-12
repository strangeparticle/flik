# flik

`flik` is a command-line interpreter for **Flik**, a Markdown-derived orchestration
language. A Flik document is simultaneously human-readable release documentation and
an executable script. Written in pure Kotlin/Native — the shipped binary has no JVM
runtime dependency.

## Building

Requires a JDK (to run Gradle and the Kotlin/Native compiler) and, on macOS, a full
**Xcode** install — the Kotlin/Native linker invokes `xcodebuild`. If `xcode-select`
points at the Command Line Tools rather than Xcode, point the toolchain at Xcode for
the invocation (no `sudo` needed):

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew build
```

Compiled binaries land under `build/bin/<target>/<debug|release>Executable/flik.kexe`.

## Usage

```sh
flik version
flik validate <root.flik.md | folder>
flik explain  <root.flik.md | folder>
flik run <root.flik.md | folder> [--project-root <dir>]
```

`validate`, `explain`, and `run` accept either a root `.flik.md` page or a **folder**
containing one — a folder resolves to its `<folder-name>.flik.md` (so
`flik validate examples/release-macos-dmg` finds `release-macos-dmg.flik.md`).

- **validate** — *links* the whole page graph from the root: parses every reachable
  page, resolves every callout (by name normalization), and reports **all** structural
  problems at once (missing pages, parse errors, cycles, missing version footers),
  each tagged with its page. No execution; needs no `--project-root`, so it runs
  anywhere (e.g. CI).
- **explain** — links, then prints the page graph and the aggregated prerequisites
  without executing.
- **run** — links, then **preflights** (checks every prerequisite across every page up
  front, reporting all the missing ones together), then executes the compiled program
  in order: `back up to file`, callouts (which copy files, edit in place, run commands,
  and verify outputs), and `restore the repository`. Stops on the first execution
  failure.

A worked example lives in `examples/release-macos-dmg/` (a macOS signed/notarized
direct-download `.dmg` release).

## Targets

Native executables: `macosArm64`, `macosX64`, `linuxX64`, `mingwX64`. Tests run on
the macOS targets.

## Test

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :macosArm64Test
```

## Project layout

Source is under `com.strangeparticle.flik`
(`src/nativeMain/kotlin/com/strangeparticle/flik/`):

- `Flik.kt` — CLI entry (Clikt: `version` / `validate` / `explain` / `run`)
- `command/` — the page model: pages, commands, page invocations, the page parser/runner, and execution context
- `program/` — whole-program linker (`compile`) and aggregate `preflight`
- `parse/` — callout-name normalization and parse errors
- `run/` — the run driver (sets up the execution directory, runs the linked program)
- `os/` — POSIX filesystem and process helpers

`examples/` holds the Flik example documents.
