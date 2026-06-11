# flik

A command-line app written in pure Kotlin/Native (no JVM runtime) using
[Clikt](https://ajalt.github.io/clikt/) for argument parsing. `flik` compiles to
self-contained native binaries — the shipped executable has no JVM dependency.

> Note: the *build* uses Gradle and the Kotlin compiler, which run on a JVM. Only
> the produced binaries are JVM-free.

## Requirements

- A JDK (to run Gradle and the Kotlin/Native compiler)
- macOS, Linux, or Windows host

## Targets

Native executables are produced for:

- `macosArm64` (Apple Silicon)
- `macosX64` (Intel macOS)
- `linuxX64`
- `mingwX64` (Windows)

From an Apple Silicon Mac, `./gradlew build` compiles all four targets, but tests
run only for the macOS targets. The Linux and Windows binaries are best verified
on their native OS (or in CI).

## Build

```sh
./gradlew build
```

Compiled binaries land under `build/bin/<target>/<debug|release>Executable/`.

## Run

```sh
# Debug build for Apple Silicon
./build/bin/macosArm64/debugExecutable/flik.kexe hello --name world
# -> Hello, world!
```

Or run via Gradle:

```sh
./gradlew runDebugExecutableMacosArm64 --args="hello --name world"
```

## Test

```sh
./gradlew allTests
```

## Project layout

- `src/nativeMain/kotlin/Main.kt` — CLI entry point and command definitions
- `src/nativeMain/kotlin/GreetingUtil.kt` — pure greeting logic (unit-tested)
- `src/nativeTest/kotlin/GreetingTest.kt` — tests
