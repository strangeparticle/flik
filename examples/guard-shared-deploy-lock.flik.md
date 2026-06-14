# Guard a Shared Deploy Lock

Demonstrates `Expect file to be empty:` as a shared-resource guard at the **start**
of a runbook. A deploy lock file is shared across operators: an empty (or absent)
lock means the resource is free, so the run proceeds and claims it. A lock that
already names another deploy means someone else holds it — the assertion fails
loudly, previewing who, before any destructive step runs.

# Pre-requisites
* shell command: `date`
* shell command: `whoami`

Environment Variables:
* DEPLOY_TARGET_ENVIRONMENT

# Procedure

## Guard: the deploy lock must be free

> Run this first, before anything irreversible. If another operator already holds
> the lock, the file is non-empty and this assertion fails — naming the lock path
> and previewing its current holder — so we stop instead of colliding.

Expect file to be empty: `${{ project_root }}/locks/deploy.lock`

## Claim the lock for this operator

Run command:

```shell
mkdir -p "${{ project_root }}/locks" && echo "held by $(whoami) for ${{ env.DEPLOY_TARGET_ENVIRONMENT }} at $(date -u +%Y-%m-%dT%H:%M:%SZ)" > "${{ project_root }}/locks/deploy.lock"
```

* capture output as: `LOCK_CLAIM_RESULT`

## Confirm the lock now names this run

Run command:

```shell
cat "${{ project_root }}/locks/deploy.lock"
```

* expect to see: `held by`
* capture output as: `CURRENT_LOCK_HOLDER`

## Release the lock

> Truncate the lock back to empty so the next operator's guard passes.

Run command:

```shell
: > "${{ project_root }}/locks/deploy.lock"
```

---

_Authored for compatibility with Flik `v0.1`._
