# Release Springboard for macOS (Direct Download DMG)

# Pre-requisites
flik version: 0.1

Environment Variables:
* APPLE_ID
* APPLE_APP_SPECIFIC_PASSWORD
* APPLE_TEAM_ID

Shell Commands:
* ./gradlew
* xcrun
* codesign
* spctl
* hdiutil
* lipo

# Release Procedure
Run these steps sequentially, in the order shown:
* back up to file `../springboard-release-backup.tar.gz`
* [Re-apply release-only repo changes]
* [Build the signed, notarized DMG]
* [Verify the DMG signature and notarization]
* restore the repository