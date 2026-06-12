# Re-Apply Release-Only Repo Changes

flik version: 0.1

The public `springboard` repo intentionally ships **unsigned** packaging: it has no
signing identities, no notarization wiring, no entitlements files, and no release
ProGuard rules. To build a signed, notarized direct-download `.dmg`, those pieces
are re-applied to the working copy temporarily.

> These changes are temporary. They are reversed at the end of the release by the
> backup restore in the top-level document — they must never be committed.

This page covers the **direct distribution** path only: it copies the `-direct`
entitlements and ProGuard rules and wires signing + notarization. It does NOT copy
App Store provisioning profiles or sandboxed entitlements.

## Copy the entitlements and ProGuard files

* Copy file from: `./resources/entitlements-direct.plist` to: `desktopApp/src/main/packaging/macos/entitlements/entitlements-direct.plist`
* Copy file from: `./resources/runtime-entitlements-direct.plist` to: `desktopApp/src/main/packaging/macos/entitlements/runtime-entitlements-direct.plist`
* Copy file from: `./resources/proguard-rules.pro` to: `desktopApp/proguard-rules.pro`

## Add the distribution-type selector

The selector lets the build choose between `app-store` and `direct` packaging. Add
it just above the `compose.desktop { ... }` block.

In file: `desktopApp/build.gradle.kts`

Find this section:

```kotlin
compose.desktop {
```

Replace it with:

```kotlin
// OS X distribution type: "app-store" or "direct".
// Pass -PosxDistributionType=direct for direct distribution (signed/notarized DMG) builds.
// Default (when omitted) is "app-store".
val osxDistributionType = project.findProperty("osxDistributionType")?.toString() ?: "app-store"
require(osxDistributionType in listOf("app-store", "direct")) {
    "Invalid osxDistributionType '$osxDistributionType'. Must be 'app-store' or 'direct'."
}
val isAppStoreBuild = osxDistributionType == "app-store"

compose.desktop {
```

Use loose whitespace matching. Preserve surrounding indentation. Make the smallest
safe edit.

## Restore the release ProGuard rules

Without this block, `desktopApp/proguard-rules.pro` is ignored and ProGuard
warnings fail the release build. Add it just before the `nativeDistributions { ... }`
block.

In file: `desktopApp/build.gradle.kts`

Find this section:

```kotlin
            nativeDistributions {
```

Replace it with:

```kotlin
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
            }

            nativeDistributions {
```

Use loose whitespace matching. Preserve surrounding indentation. Make the smallest
safe edit.

## Restore the release `macOS { ... }` block

Replace the simplified public `macOS { ... }` body with the release version that
wires entitlements, signing, and notarization. The `else` branch (direct
distribution) is the one this release uses.

In file: `desktopApp/build.gradle.kts`

Find this section:

```kotlin
macOS {
    iconFile.set(project.file("src/main/resources/icon.icns"))

    packageName = "Springboard"
    bundleID = "com.strangeparticle.springboard.core"
    appCategory = "public.app-category.developer-tools"
    minimumSystemVersion = "12.0"
}
```

Replace it with:

```kotlin
macOS {
    iconFile.set(project.file("src/main/resources/icon.icns"))

    packageName = "Springboard"
    bundleID = "com.strangeparticle.springboard.core"
    appCategory = "public.app-category.developer-tools"
    minimumSystemVersion = "12.0"

    // requires jdk 21 or higher, earlier versions have a bug that causes signed
    // app-store builds to fail
    appStore = isAppStoreBuild

    val macosPackagingDir = "./src/main/packaging/macos"

    // do not reformat these with something like the intellij xml editor, it introduces formatting that fails the build
    if (isAppStoreBuild) {
        // App Store: sandboxed entitlements with app-identifier, plus provisioning profiles
        entitlementsFile.set(project.file("$macosPackagingDir/entitlements/entitlements.plist"))
        runtimeEntitlementsFile.set(project.file("$macosPackagingDir/entitlements/runtime-entitlements.plist"))

        // IMPORTANT: strip the quarantine attribute from these files after they're downloaded from apple developer site:
        //   xattr -d com.apple.quarantine ./desktopApp/src/main/packaging/macos/provisionprofiles/embedded.provisionprofile
        //   xattr -d com.apple.quarantine ./desktopApp/src/main/packaging/macos/provisionprofiles/runtime.provisionprofile
        provisioningProfile.set(project.file("$macosPackagingDir/provisionprofiles/embedded.provisionprofile"))
        runtimeProvisioningProfile.set(project.file("$macosPackagingDir/provisionprofiles/runtime.provisionprofile"))
    } else {
        // Direct distribution: no sandbox, no app-identifier, no provisioning profiles
        entitlementsFile.set(project.file("$macosPackagingDir/entitlements/entitlements-direct.plist"))
        runtimeEntitlementsFile.set(project.file("$macosPackagingDir/entitlements/runtime-entitlements-direct.plist"))
    }

    signing {
        sign.set(true)
        identity.set("Gary Affonso")
    }

    if (!isAppStoreBuild) {
        notarization {
            appleID.set(providers.environmentVariable("APPLE_ID"))
            password.set(providers.environmentVariable("APPLE_APP_SPECIFIC_PASSWORD"))
            teamID.set(providers.environmentVariable("APPLE_TEAM_ID"))
        }
    }
}
```

Use loose whitespace matching. Preserve surrounding indentation. Make the smallest
safe edit.
