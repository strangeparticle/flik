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
  notarized direct-download `.dmg`. Demonstrates: inline prerequisites, a sequential
  procedure with `back up to file` / `restore the repository`, bare bracketed
  callouts (resolved by name normalization), `Copy file from: … to: …`,
  `In file:` / `Find this section:` / `Replace it with:` edits, `Run command:`,
  `Expect file to exist:`, `Run these checks in parallel:`, and `{{ project_root }}`.

## Validate an example

```sh
flik validate release-macos-dmg/release-macos-dmg.flik.md
```

## Writing new Flik documents

See [`../docs/flik-for-ai.md`](../docs/flik-for-ai.md) for the authoring cheat sheet
(vocabulary, the five rules, and the generate → validate → fix loop).
