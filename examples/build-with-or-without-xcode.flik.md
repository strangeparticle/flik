# Build With or Without Xcode

Demonstrates a **block-level conditional** — `If <predicate>: … Otherwise: …`. The
predicate is one of a small fixed set Flik can decide deterministically (here, whether a
command is on the `PATH`). A human or AI reading this can follow the same branch by hand;
the backtick-quoted command name is the only machine-checked part.

# Pre-requisites
* shell command: `echo`

# Procedure

## Pick a build path based on the toolchain

If the command `xcodebuild` is available:

Run command:

```shell
echo "Xcode is present — building the signed app"
```

Otherwise:

Run command:

```shell
echo "Xcode is absent — building with the command-line tools"
```

> Each branch is a single element. When a branch needs several steps, make it a page
> invocation (`* [Build without Xcode]`) and put the steps in that page — Flik links and
> preflights the branch page just like any other.

---

_Authored for compatibility with Flik `v0.1`._
