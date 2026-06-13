# Stage User Config

Stages a user's shell profile from their home directory into the project, then records
which account it came from. This demonstrates environment-variable interpolation in
non-shell contexts: an `Expect file to exist:` path and a `Copy file from:`/`to:` path
both read `${{ env.HOME }}` and `${{ env.STAGE_PROFILE_NAME }}` directly, with no shell
involved.

# Pre-requisites
* environment variable: `HOME`
  * the running user's home directory; provided by every login shell
* environment variable: `STAGE_PROFILE_NAME`
  * the profile filename to stage from `${{ env.HOME }}`, e.g. `.zshrc`

# Procedure

## Confirm the source profile is present

The path is built entirely from environment variables — Flik resolves `${{ env.HOME }}`
and `${{ env.STAGE_PROFILE_NAME }}` before touching the filesystem, so no shell glob or
`$VAR` expansion is needed.

Expect file to exist: `${{ env.HOME }}/${{ env.STAGE_PROFILE_NAME }}`

## Stage it into the project

`from` is an absolute, env-interpolated source in the user's home; `to` is resolved
against the project root.

Copy file from: `${{ env.HOME }}/${{ env.STAGE_PROFILE_NAME }}` to: `staged/profile.txt`

## Record the source account

> Inside this fenced shell block, the plain `$USER` is ordinary shell expansion and is
> left untouched; only the `${{ env.HOME }}` form is Flik interpolation.

Run command:

```shell
printf 'staged %s from %s for $USER\n' "${{ env.STAGE_PROFILE_NAME }}" "${{ env.HOME }}" > staged/source.txt
```

## Verify the record landed

Expect file to exist: `${{ project_root }}/staged/source.txt`

---

_Authored for compatibility with Flik `v0.1`._
