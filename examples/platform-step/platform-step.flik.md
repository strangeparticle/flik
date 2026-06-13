# Platform-Specific Step

Demonstrates a **switch** — `Depending on \`<selector>\`:` with one branch per value. We
detect the operating system, capture it, and run the matching branch. Each branch here is a
page invocation, so the switch reads as a menu of named procedures; `otherwise` is the
default when nothing matches.

# Pre-requisites
* shell command: `uname`

# Procedure

## Detect the operating system

Run command:

```shell
uname -s
```

* capture output as: `os`

## Run the step for this platform

Depending on `${{ capture.os }}`:

* `Darwin`: [Do the macOS step]
* `Linux`: [Do the Linux step]
* otherwise: [Do the generic step]

---

_Authored for compatibility with Flik `v0.1`._
