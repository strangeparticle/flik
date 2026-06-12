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
flik validate <entry.flik.md>
flik run <entry.flik.md> [--project-root <dir>]
```

- **validate** — parses an entry document, resolves its bracketed callouts to
  sibling `.flik.md` files via name normalization, and reports structure without
  executing anything.
- **run** — checks prerequisites (required env vars and commands), then executes the
  procedure in order: `back up to file`, callouts (which copy files, edit files in
  place, run commands, and verify outputs), and `restore the repository`. Stops on
  the first failure.

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

- `Flik.kt` — CLI entry (Clikt: `version` / `validate` / `run`)
- `command/` — the page model: pages, commands, page invocations, the page parser/runner, and execution context
- `parse/` — callout-name normalization and parse errors
- `run/` — the run driver (sets up the execution directory, runs the root page)
- `os/` — POSIX filesystem and process helpers
- `validate/` — page validation

`examples/` holds the Flik example documents.
