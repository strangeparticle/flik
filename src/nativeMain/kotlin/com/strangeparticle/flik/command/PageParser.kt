package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.parse.FlikParseException

private const val ENV_LABEL = "Environment Variables:"
private const val COMMANDS_LABEL = "Shell Commands:"

// Matches `Flik `v0.1`` / `Flik v1.0.0` in the version footer. The `v` + digit
// requirement means it does NOT match the word "version".
private val FLIK_VERSION_REGEX = Regex("flik\\s+`?(v\\d[\\w.+\\-]*)`?", RegexOption.IGNORE_CASE)

/**
 * Parses a Flik page. The body is every recognized element in document order;
 * unrecognized lines — headings, prose, blockquotes, blank lines, prerequisite
 * bullets, the version footer — are skipped.
 *
 * Prerequisites (declared on any page, checked on entry) are env vars and shell
 * commands only — Flik itself is never a prerequisite, because the page must be
 * followable by a human or AI without Flik installed. They may be inline bullets
 * (`* shell command: \`x\``, `* environment variable: \`X\``) or grouped under a
 * `Shell Commands:` / `Environment Variables:` label.
 *
 * The Flik version the page was authored for is declared in a footer line containing
 * `Flik \`v<version>\`` (full or partial semver: v1, v1.0.0).
 */
fun parsePage(source: String): Page {
    val lines = source.lines()

    val title = lines.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")?.trim()
        ?: throw FlikParseException("page has no '# Title' heading")

    val elements = mutableListOf<PageElement>()
    var index = 0
    while (index < lines.size) {
        val parsed = pageElementParsers.firstNotNullOfOrNull { it.tryParse(lines, index) }
        if (parsed != null) {
            elements.add(parsed.element)
            index = parsed.nextIndex
        } else {
            index++
        }
    }

    return Page(
        title = title,
        flikVersion = pageFlikVersion(lines),
        requiredEnvironmentVariables =
            collectBulletsAfterLabel(lines, ENV_LABEL) +
                inlinePrerequisiteValues(lines, "environment variable:"),
        requiredShellCommands =
            collectBulletsAfterLabel(lines, COMMANDS_LABEL) +
                inlinePrerequisiteValues(lines, "shell command:"),
        elements = elements,
    )
}

/** The Flik version from the page's footer (e.g. `Flik \`v0.1\``), or null. */
private fun pageFlikVersion(lines: List<String>): String? =
    lines.firstNotNullOfOrNull { FLIK_VERSION_REGEX.find(it)?.groupValues?.get(1) }

/** Every value from `* <label> \`value\`` inline prerequisite bullets. */
private fun inlinePrerequisiteValues(lines: List<String>, label: String): List<String> =
    lines.mapNotNull { line ->
        val content = bulletOrLine(line)
        if (!content.startsWith(label, ignoreCase = true)) {
            null
        } else {
            content.substringAfter(":").trim().trim('`').trim().ifEmpty { null }
        }
    }

/**
 * Collects consecutive `* item` bullets that follow a `label` line. Blank lines
 * before the first bullet are skipped; the first blank or non-bullet line after the
 * bullets ends the group.
 */
private fun collectBulletsAfterLabel(lines: List<String>, label: String): List<String> {
    val startIndex = lines.indexOfFirst { it.trim() == label }
    if (startIndex == -1) return emptyList()

    val bullets = mutableListOf<String>()
    var index = startIndex + 1
    while (index < lines.size) {
        val raw = lines[index]
        val trimmed = raw.trim()
        when {
            // a top-level (unindented) bullet is a value
            raw.startsWith("* ") -> bullets.add(trimmed.removePrefix("* ").trim().trim('`').trim())
            // an indented sub-bullet is a human comment — ignore it, stay in the group
            trimmed.startsWith("* ") -> Unit
            // tolerate blank lines before the first value
            trimmed.isEmpty() && bullets.isEmpty() -> Unit
            else -> break
        }
        index++
    }
    return bullets
}
