package com.strangeparticle.flik.command

import com.strangeparticle.flik.parse.FlikParseException

private const val ENV_LABEL = "Environment Variables:"
private const val COMMANDS_LABEL = "Shell Commands:"

/**
 * Parses a Flik page. Prerequisites are read from the labelled bullet lists; the body
 * is every recognized element in document order (each line is offered to the
 * registered parsers). Unrecognized lines — headings, prose, blockquotes, blank
 * lines, and the prerequisite bullets themselves — are skipped.
 */
fun parsePage(source: String): Page {
    val lines = source.lines()

    val title = lines.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")?.trim()
        ?: throw FlikParseException("page has no '# Title' heading")
    val flikVersion = lines
        .firstOrNull { it.trim().startsWith("flik version:", ignoreCase = true) }
        ?.substringAfter(":")?.trim()

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
        flikVersion = flikVersion,
        requiredEnvironmentVariables = collectBulletsAfterLabel(lines, ENV_LABEL),
        requiredShellCommands = collectBulletsAfterLabel(lines, COMMANDS_LABEL),
        elements = elements,
    )
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
            bullets.add(line.removePrefix("* ").trim())
        } else if (line.isEmpty() && bullets.isEmpty()) {
            // tolerate blank lines between the label and the first bullet
        } else {
            break
        }
        index++
    }
    return bullets
}
