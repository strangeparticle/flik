package com.strangeparticle.flik.command.util

import com.strangeparticle.flik.command.FlikExecutionException

private val PROJECT_ROOT_REGEX = Regex("\\$\\{\\{\\s*project_root\\s*}}")
private val CAPTURE_REGEX = Regex("\\$\\{\\{\\s*capture\\.([A-Za-z_][A-Za-z0-9_]*)\\s*}}")

/**
 * Substitutes Flik interpolation tokens:
 *  - `${'$'}{{ project_root }}` -> [projectRoot]
 *  - `${'$'}{{ capture.NAME }}` -> the value bound by an earlier `capture output as: NAME` modifier
 *
 * Plain shell `${'$'}VAR` / `${'$'}{VAR}` (single brace) is left untouched. An
 * `${'$'}{{ capture.NAME }}` whose NAME was never captured fails the run.
 */
fun interpolate(text: String, projectRoot: String, captures: Map<String, String> = emptyMap()): String {
    val withProjectRoot = text.replace(PROJECT_ROOT_REGEX, projectRoot)
    return CAPTURE_REGEX.replace(withProjectRoot) { match ->
        val name = match.groupValues[1]
        captures[name]
            ?: throw FlikExecutionException("unknown capture: \"$name\" (no earlier command captured it)")
    }
}
