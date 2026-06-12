package command

import parse.FlikParseException

/** A parsed stage sub-document: its heading metadata and ordered steps. */
data class StageDocument(
    val title: String,
    val flikVersion: String?,
    val steps: List<StageStep>,
)

/**
 * Parses a stage sub-document by trying each registered [StageStepParser] at every
 * line. Unrecognized lines (headings, prose, blockquotes) are skipped.
 */
fun parseStageDocument(source: String): StageDocument {
    val lines = source.lines()

    val title = lines.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")?.trim()
        ?: throw FlikParseException("sub-document has no '# Title' heading")
    val flikVersion = lines
        .firstOrNull { it.trim().startsWith("flik version:", ignoreCase = true) }
        ?.substringAfter(":")?.trim()

    val steps = mutableListOf<StageStep>()
    var index = 0
    while (index < lines.size) {
        val parsed = stageStepParsers.firstNotNullOfOrNull { it.tryParse(lines, index) }
        if (parsed != null) {
            steps.add(parsed.step)
            index = parsed.nextIndex
        } else {
            index++
        }
    }

    return StageDocument(title, flikVersion, steps)
}
