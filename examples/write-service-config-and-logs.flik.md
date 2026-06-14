# Write Service Config and Logs

Demonstrates the three declarative file-writing elements in one believable runbook: render a
service's runtime config from scratch, append a line to an audit log that must already exist,
and create-or-append to a deploy history that may or may not exist yet. Each takes a path in
backticks followed by a fenced block whose body is the file content. Block bodies interpolate
exactly like shell blocks, so `${{ env.SERVICE_NAME }}`, `${{ env.DEPLOY_REGION }}`, and
`${{ capture.RELEASE_VERSION }}` all expand inline.

# Pre-requisites
* environment variable: `SERVICE_NAME`
  * the service being deployed, e.g. `billing-api`
* environment variable: `DEPLOY_REGION`
  * the target region, e.g. `us-east-1`

# Procedure

## Capture the release version

Run command:

```shell
git describe --tags --always
```

* capture output as: `RELEASE_VERSION`

## Render the runtime config

`Write to file:` overwrites the file, creating it and any missing parent directories. This is
the right element for generated config that should fully replace whatever was there before.

Write to file: `build/config/runtime.env`

```text
SERVICE_NAME=${{ env.SERVICE_NAME }}
DEPLOY_REGION=${{ env.DEPLOY_REGION }}
RELEASE_VERSION=${{ capture.RELEASE_VERSION }}
CONFIG_ROOT=${{ project_root }}/build/config
```

## Ensure the audit log exists

`Append to file:` requires the file to already exist and errors otherwise — it never creates
it. Seed the audit log first so the append below is guaranteed a target.

Run command:

```shell
mkdir -p build/logs && touch build/logs/audit.log
```

## Append an audit entry

Append to file: `build/logs/audit.log`

```text
deployed ${{ env.SERVICE_NAME }} (${{ capture.RELEASE_VERSION }}) to ${{ env.DEPLOY_REGION }}
```

## Record deploy history

`Create or append to file:` appends when the file is present and creates it (with missing
parent directories) when it is absent — ideal for an append-only history that starts empty on
a fresh checkout.

Create or append to file: `build/history/deploys.log`

```text
${{ env.DEPLOY_REGION }} ${{ env.SERVICE_NAME }} ${{ capture.RELEASE_VERSION }}
```

---

_Authored for compatibility with Flik `v0.1`._
