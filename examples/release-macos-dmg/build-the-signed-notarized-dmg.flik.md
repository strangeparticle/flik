# Build the Signed, Notarized DMG

flik version: 0.1

With the release-only changes re-applied, build the direct-distribution `.dmg`. The
`notarizeReleaseDmg` task signs the `.app` bundle, submits it to Apple for
notarization, waits for the result, and packages the notarized app into a DMG.

> This signs and notarizes the `.app` bundle *inside* the DMG — it does not sign
> the DMG container itself.

## Clean previous build artifacts

Start from a clean output directory so a stale artifact can't be mistaken for a
fresh one.

Run command:

```shell
rm -rf ./desktopApp/build/compose/binaries/main-release/*
```

## Build and notarize

Run command:

```shell
./gradlew :desktopApp:notarizeReleaseDmg -PosxDistributionType=direct --no-configuration-cache
```

`--no-configuration-cache` is required: the `notarizeReleaseDmg` task is not
compatible with Gradle's configuration cache (it fails serializing the notary
settings, reporting that the deprecated `ascProvider` option "was replaced by
teamID"). Wait for completion.

## Verify the DMG was produced

Expect file to exist: `{{ project_root }}/desktopApp/build/compose/binaries/main-release/dmg/Springboard-4.10.0.dmg`
