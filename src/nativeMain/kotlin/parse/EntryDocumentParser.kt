package parse

import model.EntryDocument
import model.ProcedureStep

private const val PROCEDURE_LABEL = "Run these steps sequentially, in the order shown:"
private const val ENV_LABEL = "Environment Variables:"
private const val COMMANDS_LABEL = "Shell Commands:"
private const val BACKUP_PREFIX = "back up to file"
private val CALLOUT_REGEX = Regex("^\\[(.+)]$")

/** Parses the source of a top-level entry `.flik.md` document into an [EntryDocument]. */
fun parseEntryDocument(source: String): EntryDocument {
    val lines = source.lines()

    val title = lines.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")?.trim()
        ?: throw FlikParseException("Document has no '# Title' heading")

    val flikVersion = lines
        .firstOrNull { it.trim().startsWith("flik version:", ignoreCase = true) }
        ?.substringAfter(":")?.trim()

    val procedure = collectBulletsAfterLabel(lines, PROCEDURE_LABEL).map { parseProcedureStep(it) }

    return EntryDocument(
        title = title,
        flikVersion = flikVersion,
        requiredEnvironmentVariables = collectBulletsAfterLabel(lines, ENV_LABEL),
        requiredShellCommands = collectBulletsAfterLabel(lines, COMMANDS_LABEL),
        procedure = procedure,
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

private fun parseProcedureStep(text: String): ProcedureStep {
    if (text.equals("restore the repository", ignoreCase = true)) {
        return ProcedureStep.RestoreRepository
    }
    if (text.startsWith(BACKUP_PREFIX, ignoreCase = true)) {
        val path = text.substring(BACKUP_PREFIX.length).trim().trim('`').trim()
        if (path.isEmpty()) throw FlikParseException("'$BACKUP_PREFIX' step has no path")
        return ProcedureStep.BackUpToFile(path)
    }
    val callout = CALLOUT_REGEX.matchEntire(text)
    if (callout != null) {
        return ProcedureStep.Callout(callout.groupValues[1].trim())
    }
    throw FlikParseException("Unrecognized procedure step: \"$text\"")
}
