# Stamp Build Metadata

Demonstrates the modifiers a `Run command:` owns — **capture** its output for reuse,
**expect** something in its output, and **fall back** to another command if it fails.
We derive a build tag, record it, verify it, and checksum it with a cross-platform
fallback (the Linux tool name, falling back to the macOS one).

# Pre-requisites
* shell command: `date`

# Procedure

## Capture the build year

Run command:

```shell
date +%Y
```

* capture output as: `BUILD_YEAR`

## Record the build tag (using the capture)

Run command:

```shell
echo "build-${{ capture.BUILD_YEAR }}" > "${{ project_root }}/BUILD_TAG.txt"
```

## Read it back, assert it, and capture it

A single command can both assert on and capture its output.

Run command:

```shell
cat "${{ project_root }}/BUILD_TAG.txt"
```

* expect to see: `build-`
* capture output as: `BUILD_TAG`

## Checksum it (Linux tool name, falling back to the macOS name)

> `sha256sum` is the GNU/Linux name; macOS ships `shasum`. Whichever your platform
> provides runs; if the first command fails (e.g. the tool isn't installed), the
> fallback runs instead.

Run command:

```shell
sha256sum "${{ project_root }}/BUILD_TAG.txt"
```

If the command fails:

```shell
shasum -a 256 "${{ project_root }}/BUILD_TAG.txt"
```

---

_Authored for compatibility with Flik `v0.1`._
