# Verify the DMG Signature and Notarization


Confirm the built artifact is correctly signed by the expected Developer ID,
accepted by Gatekeeper as notarized, and built for the expected architectures. An
ad-hoc signature (the silent fallback when real signing fails) must be treated as a
failure, not a pass.

## Mount the DMG

Run command:

```shell
hdiutil attach "${{ project_root }}/desktopApp/build/compose/binaries/main-release/dmg/Springboard-4.10.0.dmg"
```

## Run the verification checks

Run these checks in parallel:

* **Signature** — run `codesign --display --verbose=4 /Volumes/Springboard/Springboard.app` and expect the output to contain `Authority=Developer ID Application: Gary Affonso`. It must NOT be an ad-hoc signature.
* **Notarization** — run `spctl --assess --type exec --verbose=3 /Volumes/Springboard/Springboard.app/` and expect `accepted` with `source=Notarized Developer ID`.
* **Architectures** — run `lipo -archs /Volumes/Springboard/Springboard.app/Contents/MacOS/Springboard` and report the supported architectures (expect `arm64` and `x86_64`).

## Unmount the DMG

Run command:

```shell
hdiutil detach "/Volumes/Springboard"
```

---

_Authored for compatibility with Flik `v0.1`._
