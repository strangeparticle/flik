package com.strangeparticle.flik.command.util

/**
 * The path and block body of a file-writing element: a label line of the form
 * `<Label>: \`path\`` followed by a fenced code block whose body is the content.
 */
data class FileWriteParse(val path: String, val content: String, val nextIndex: Int)

/**
 * Parses a file-writing element that starts with [label] (case-insensitive) followed by a
 * backtick-quoted path, then a fenced code block. Returns null if [lines] at [index] is not
 * this label, so the registry can try the next parser. Throws (via [readFencedBlock]) if the
 * label matches but no fenced block follows.
 */
fun parseFileWriteElement(label: String, lines: List<String>, index: Int): FileWriteParse? {
    val regex = Regex("$label: `(.+?)`", RegexOption.IGNORE_CASE)
    val match = regex.matchEntire(bulletOrLine(lines[index])) ?: return null
    val path = match.groupValues[1]
    val (content, nextIndex) = readFencedBlock(lines, index + 1)
    return FileWriteParse(path, content, nextIndex)
}

/**
 * Ensures [content] ends with exactly one trailing newline, matching the heredoc behavior
 * used elsewhere (a fenced block's body has its final newline stripped during parsing, so
 * file-writing commands re-add it).
 */
fun withTrailingNewline(content: String): String {
    if (content.endsWith("\n")) return content
    return "$content\n"
}
