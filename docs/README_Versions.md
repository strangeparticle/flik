# flik Versioning

How flik is versioned, where the version lives, and how to bump it. Point an AI
agent at this file when preparing a release.

## Scheme

flik uses [Semantic Versioning](https://semver.org): `MAJOR.MINOR.PATCH`.

- **Pre-1.0 is allowed.** While the API/behavior is still settling, versions stay
  in the `0.y.z` range (e.g. `0.1.0`), where a `0.x` line makes no stability
  promises.
- **Optional identifiers are allowed.** A pre-release suffix (`-rc.1`, `-beta.2`)
  and/or build metadata (`+abc1234`) may follow the triplet, e.g. `1.0.0-rc.1`.

## The one place the version lives

The version is declared in **exactly one** spot:

```
# gradle.properties
flikVersion=0.1.0
```

Nothing else holds the literal version string. Everything below is derived from
this line.

## How to bump the version

1. Edit the single line in `gradle.properties` (`flikVersion=...`).
2. Build. That's it — the runtime constant and the Gradle project version both
   pick up the new value automatically.

Do **not** hand-edit any `.kt` file to change the version; the runtime constant is
generated.

## How it reaches the runtime

A Gradle task, **`GenerateKotlinVersionFile`**, reads `flikVersion` and writes:

```
build/generated/flikVersion/com/strangeparticle/flik/FlikVersion.kt
  → object FlikVersion { const val VERSION = "0.1.0" }
```

That directory is a source directory of `nativeMain`, and the task is wired to run
before every native compile (main and test). The generated file lives under
`build/` and is **git-ignored**, so the version is never duplicated in tracked
source and cannot drift from `gradle.properties`.

Runtime code reads `FlikVersion.VERSION`.

## How it surfaces to users

Both print the same string (clikt's standard version format):

```
$ flik version
flik version 0.1.0

$ flik --version
flik version 0.1.0
```

## How it reaches build time

`build.gradle.kts` sets the Gradle project version from `flikVersion`
(`version = flikVersion`), so any current or future build/packaging logic can read
the project version instead of re-parsing the file.

## Release / signing / notarization (deferred)

There is no release, code-signing, or notarization pipeline yet; that work is
tracked separately. One thing to know in advance:

Apple's bundle version fields (`CFBundleShortVersionString`, `CFBundleVersion`)
accept only up to three dot-separated integers — they reject `-prerelease` and
`+build` suffixes. This does **not** restrict flik's version:

- A standalone signed/notarized CLI binary (or a `.pkg`/`.dmg`) has no Info.plist
  bundle-version field, so the full SemVer string is fine as-is.
- If flik is ever wrapped in an `.app` bundle, the packaging step will derive the
  numeric core (`MAJOR.MINOR.PATCH`, suffixes stripped) for Apple's fields, while
  `flik --version` keeps displaying the full SemVer string.

So richer versions like `1.0.0-rc.1` are safe to use now.
