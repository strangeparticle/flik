# Poll Until Deployment Is Healthy

Demonstrates `Poll by running:` — re-run a command on an interval until its output
contains an expected example, or give up after a timeout. Flik runs the very `curl` a
human would run by hand, so the page is followable without Flik.

# Pre-requisites
* shell command: `curl`

# Procedure

## Wait for the deployment's health endpoint to report healthy

> The match is a whitespace-insensitive **substring**, so write the smallest distinctive
> sub-portion of the response — not a full braced object that surrounding fields would
> split. The polling typically clears in under a minute; if it runs past the timeout,
> something is wrong with the deploy.

Poll by running:

```shell
curl -sf https://status.example.com/healthz
```

Until the output contains:

```json
"status": "healthy"
```

* time out after: `5m`
* check every: `10s`
* capture output as: `health_response`

---

_Authored for compatibility with Flik `v0.1`._
