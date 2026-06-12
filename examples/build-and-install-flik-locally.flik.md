# Build and Install flik Locally

Builds the optimized native `flik` binary and installs it onto your `PATH` at
`~/.local/bin/flik`.

# Pre-requisites
* shell command: `./gradlew`
  * provided by the flik project — no need to install it
* jdk 17
  * required only to build the flik binary, not to run it (Gradle 9.1 needs Java 17+)

# Procedure

## Build the release binary

> The Kotlin/Native linker invokes `xcodebuild`. `DEVELOPER_DIR` points it at Xcode,
> which is needed when `xcode-select` is set to the Command Line Tools.

Run command:

```shell
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :linkReleaseExecutableMacosArm64
```

## Verify the binary was produced

Expect file to exist: `${{ project_root }}/build/bin/macosArm64/releaseExecutable/flik.kexe`

## Install onto PATH

`~/.local/bin` is a user-writable directory already on `PATH` (no `sudo` needed).
Install atomically — write a temp file, then `mv` it into place. A plain in-place
`cp` would overwrite the binary that may be running this very procedure (flik
installing flik), which macOS then kills on its next launch (`Killed: 9`). A `mv`
gives the new binary a fresh inode, leaving the running process untouched.

Run command:

```shell
mkdir -p ~/.local/bin && cp build/bin/macosArm64/releaseExecutable/flik.kexe ~/.local/bin/flik.new && chmod +x ~/.local/bin/flik.new && mv -f ~/.local/bin/flik.new ~/.local/bin/flik
```

## Verify the installed binary

* run command: `~/.local/bin/flik version`
* expect to see `flik`

---

_Authored for compatibility with Flik `v0.1`._
