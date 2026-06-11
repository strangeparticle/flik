package run

private val PROJECT_ROOT_REGEX = Regex("\\{\\{\\s*project_root\\s*}}")

/** Substitutes the only supported interpolation, `{{ project_root }}`, with [projectRoot]. */
fun interpolate(text: String, projectRoot: String): String =
    text.replace(PROJECT_ROOT_REGEX, projectRoot)
