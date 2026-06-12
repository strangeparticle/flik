package command

import parse.FlikParseException

/** A line's content with surrounding whitespace and an optional leading `* ` bullet removed. */
fun bulletOrLine(line: String): String = line.trim().removePrefix("* ").trim()

/** Returns the index >= [from] of the first line whose trimmed text equals [target], or -1. */
fun indexOfTrimmed(lines: List<String>, from: Int, target: String): Int {
    for (j in from until lines.size) {
        if (lines[j].trim() == target) return j
    }
    return -1
}

/**
 * Reads the next fenced ``` block at or after [from]. Returns the block body and the
 * index just past the closing fence.
 */
fun readFencedBlock(lines: List<String>, from: Int): Pair<String, Int> {
    var j = from
    while (j < lines.size && lines[j].trim().isEmpty()) j++
    if (j >= lines.size || !lines[j].trim().startsWith("```")) {
        throw FlikParseException("expected a fenced code block near line ${j + 1}")
    }
    val body = mutableListOf<String>()
    j++
    while (j < lines.size && !lines[j].trim().startsWith("```")) {
        body.add(lines[j])
        j++
    }
    if (j >= lines.size) throw FlikParseException("unterminated fenced code block")
    return body.joinToString("\n") to (j + 1)
}
