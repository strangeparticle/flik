package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.parse.FlikParseException

private const val ENV_LABEL = "Environment Variables:"
private const val COMMANDS_LABEL = "Shell Commands:"

/**
 * Parses a Flik page. Prerequisites may be written two ways:
 *   - inline, one bullet each:  `* flik version: \`0.1\``,
 *     `* shell command: \`./gradlew\``, `* environment variable: \`APPLE_ID\``
 *   - grouped, for several of a kind:  a `Shell Commands:` / `Environment Variables:`
 *     label followed by a `* value` bullet list
 *
 * The body is every recognized element in document order; unrecognized lines —
 * headings, prose, blockquotes, blank lines, prerequisite bullets — are skipped.
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
        flikVersion = firstPrerequisiteValue(lines, "flik version:"),
        requiredEnvironmentVariables =
            collectBulletsAfterLabel(lines, ENV_LABEL) +
                inlinePrerequisiteValues(lines, "environment variable:"),
        requiredShellCommands =
            collectBulletsAfterLabel(lines, COMMANDS_LABEL) +
                inlinePrerequisiteValues(lines, "shell command:"),
        elements = elements,
    )
}

/** The value from the first `<label> value` prerequisite line (bullet or bare), or null. */
private fun firstPrerequisiteValue(lines: List<String>, label: String): String? =
    lines.firstNotNullOfOrNull { prerequisiteValue(bulletOrLine(it), label) }

/** Every value from `* <label> \`value\`` inline prerequisite bullets. */
private fun inlinePrerequisiteValues(lines: List<String>, label: String): List<String> =
    lines.mapNotNull { prerequisiteValue(bulletOrLine(it), label) }

/** If [content] is `<label> value`, returns the unquoted value; otherwise null. */
private fun prerequisiteValue(content: String, label: String): String? {
    if (!content.startsWith(label, ignoreCase = true)) return null
    return content.substringAfter(":").trim().trim('`').trim().ifEmpty { null }
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
        val line = lines[index].trim()
        if (line.startsWith("* ")) {
            bullets.add(line.removePrefix("* ").trim().trim('`').trim())
        } else if (line.isEmpty() && bullets.isEmpty()) {
            // tolerate blank lines between the label and the first bullet
        } else {
            break
        }
        index++
    }
    return bullets
}
