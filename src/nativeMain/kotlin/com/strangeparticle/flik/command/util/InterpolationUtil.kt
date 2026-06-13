package com.strangeparticle.flik.command.util

import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.os.environmentVariable

private val PROJECT_ROOT_REGEX = Regex("\\$\\{\\{\\s*project_root\\s*}}")
private val CAPTURE_REGEX = Regex("\\$\\{\\{\\s*capture\\.([A-Za-z_][A-Za-z0-9_]*)\\s*}}")

// Matches `${{ env.NAME }}`. NAME is a POSIX-style env var name: a letter or underscore
// followed by letters, digits, or underscores. The `env.` prefix is what distinguishes a
// Flik env reference from a plain shell `$VAR` / `${VAR}` (never the double-brace form, so
// it is left untouched) and from the `capture.` namespace.
private val ENVIRONMENT_REGEX = Regex("\\$\\{\\{\\s*env\\.([A-Za-z_][A-Za-z0-9_]*)\\s*}}")

/**
 * Substitutes Flik interpolation tokens:
 *  - `${'$'}{{ project_root }}` -> [projectRoot]
 *  - `${'$'}{{ capture.NAME }}` -> the value bound by an earlier `capture output as: NAME` modifier
 *  - `${'$'}{{ env.NAME }}` -> the value of the NAME environment variable
 *
 * Plain shell `${'$'}VAR` / `${'$'}{VAR}` (single brace) is left untouched. Missing values
 * are a hard backstop: an `${'$'}{{ capture.NAME }}` that was never captured, or an
 * `${'$'}{{ env.NAME }}` whose variable is unset, fails the run. In normal use `preflight`
 * reports an unset `${'$'}{{ env.NAME }}` before execution begins, so interpolation rarely
 * reaches a missing variable — the throw is the last-resort guard against a silent empty
 * substitution, not the primary report.
 */
fun interpolate(text: String, projectRoot: String, captures: Map<String, String> = emptyMap()): String {
    val withProjectRoot = text.replace(PROJECT_ROOT_REGEX, projectRoot)
    val withCaptures = CAPTURE_REGEX.replace(withProjectRoot) { match ->
        val name = match.groupValues[1]
        captures[name]
            ?: throw FlikExecutionException("unknown capture: \"$name\" (no earlier command captured it)")
    }
    return ENVIRONMENT_REGEX.replace(withCaptures) { match ->
        val name = match.groupValues[1]
        environmentVariable(name)
            ?: throw FlikExecutionException("environment variable not set: \"$name\" (referenced via \${{ env.$name }})")
    }
}

/**
 * Every distinct environment variable name referenced via `${'$'}{{ env.NAME }}` in [text],
 * in first-seen order. Used by `preflight` to check, up front, that each referenced variable
 * is set — independent of where the reference appears (a path, a run command, an edit block).
 */
fun environmentReferences(text: String): List<String> =
    ENVIRONMENT_REGEX.findAll(text)
        .map { match -> match.groupValues[1] }
        .distinct()
        .toList()
