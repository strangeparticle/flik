# Flik examples (cookbook)

Validated, known-good Flik documents. This folder serves two audiences:

- **Humans** — worked references for how a real Flik procedure is structured.
- **AI generators** — few-shot fuel. A model pattern-matches against a real,
  validated example far more reliably than against rules alone, so each entry here
  makes the next generated document easier and more correct.

Every example should pass `flik validate` cleanly. Treat that as the bar for adding
a new one.

## Index

- [`release-macos-dmg/`](./release-macos-dmg/) — release a macOS app as a signed,
  notarized direct-download `.dmg`. A root page that declares per-page prerequisites,
  backs up, invokes sub-pages (`back up to file` / `restore the repository`), and
  restores. Demonstrates: bare bracketed page invocations (resolved by name
  normalization), `Copy file from: … to: …`, `In file:` / `Find this section:` /
  `Replace it with:` edits, `Run command:`, `Expect file to exist:`,
  `Run these checks in parallel:`, and `${{ project_root }}`.
- [`build-and-install-flik-locally.flik.md`](./build-and-install-flik-locally.flik.md)
  — build the release `flik` binary and install it onto `PATH` (Flik installing
  Flik). A single self-contained page: per-page prerequisites, `Run command:`,
  `Expect file to exist:`, and an atomic temp-then-`mv` install.
- [`write-service-config-and-logs.flik.md`](./write-service-config-and-logs.flik.md)
  — generate a service's runtime config and write to its logs during a deploy. A single
  self-contained page demonstrating all three declarative file-writing elements:
  `Write to file:` (overwrite, auto-creating parent directories), `Append to file:`
  (append to an existing file, error if absent), and `Create or append to file:`
  (append when present, create when absent). Each uses a backtick path plus a fenced
  block whose body interpolates `${{ env.NAME }}`, `${{ capture.NAME }}`, and
  `${{ project_root }}`.
- [`guard-shared-deploy-lock.flik.md`](./guard-shared-deploy-lock.flik.md) — use
  `Expect file to be empty:` as a shared-resource guard at the start of a runbook.
  An empty (or absent) deploy lock means the resource is free; a non-empty lock fails
  loudly with a preview of its current holder before any destructive step runs.
  Demonstrates: `Expect file to be empty:`, `${{ env.NAME }}`, `${{ project_root }}`,
  and `capture output as:`.
- [`poll-until-deployment-healthy.flik.md`](./poll-until-deployment-healthy.flik.md)
  — wait for an external system using `Poll by running:` + `Until the output contains:`:
  re-run a `curl` until its output contains an expected sub-portion, with `time out
  after:` / `check every:` and `capture output as:`.

## Validate an example

```sh
flik validate release-macos-dmg/release-macos-dmg.flik.md
```

## Writing new Flik documents

See [`../docs/flik-for-ai.md`](../docs/flik-for-ai.md) for the authoring cheat sheet
(vocabulary, the five rules, and the generate → validate → fix loop).
