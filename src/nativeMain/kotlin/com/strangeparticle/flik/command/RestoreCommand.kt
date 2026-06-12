package com.strangeparticle.flik.command

import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.singleQuote

/**
 * Restore the project from a backup. Two forms:
 *  - `restore the repository` — restore the most recent backup taken by the current page.
 *  - `restore from backup <name>` — restore a specific named backup.
 */
class RestoreCommand(val backupName: String?) : Command {
    override fun execute(context: ExecutionContext) {
        val path = if (backupName != null) {
            context.backupPathFor(backupName)
        } else {
            context.mostRecentBackupForCurrentPage()?.filePath
                ?: throw FlikExecutionException("restore requested but this page took no backup")
        }
        context.log("restoring project from $path")
        val result = runShellCommand("tar xzf ${singleQuote(path)}", context.projectRoot)
        if (result.exitCode != 0) throw FlikExecutionException("restore failed:\n${result.output}")
    }

    companion object : PageElementParser {
        private val NAMED_REGEX = Regex("restore from backup (.+)", RegexOption.IGNORE_CASE)
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val content = bulletOrLine(lines[index])
            if (content.equals("restore the repository", ignoreCase = true)) {
                return ParsedElement(RestoreCommand(null), index + 1)
            }
            val named = NAMED_REGEX.matchEntire(content) ?: return null
            val name = named.groupValues[1].trim().trim('`').trim()
            return ParsedElement(RestoreCommand(name), index + 1)
        }
    }
}
