# Deploy by Environment

Demonstrates a **switch on an environment variable** — the canonical "pick a path based on
where you're deploying" runbook. `${{ env.STAGE }}` selects one branch, and each branch is
its own page. `otherwise` covers an unset or unexpected value; independently, preflight
reports an unset `STAGE` up front, because the selector references it (you would get that
check even if `STAGE` were not also declared as a prerequisite below).

# Pre-requisites
* environment variable: `STAGE`
  * which environment to deploy to: `production`, `dev`, or `staging`

# Procedure

## Deploy to the selected environment

Depending on `${{ env.STAGE }}`:

* `production`: [Deploy to production]
* `dev`: [Deploy to dev]
* `staging`: [Deploy to staging]
* otherwise: [Choose an environment]

---

_Authored for compatibility with Flik `v0.1`._
