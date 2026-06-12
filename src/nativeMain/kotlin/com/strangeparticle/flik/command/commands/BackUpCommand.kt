package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.singleQuote
import com.strangeparticle.flik.parse.FlikParseException

/**
 * `back up to file <name>` — back up the project (excluding build output) into the
 * run's backups directory as <name>. The backup is attributed to the current page so
 * a later `restore the repository` on that page can find it.
 */
class BackUpCommand(val fileName: String) : Command {
    override fun execute(context: ExecutionContext) {
        val destination = context.backupPathFor(fileName)
        context.log("backing up project to backups/$fileName")
        val result = runShellCommand(
            "tar czf ${singleQuote(destination)} --exclude=./build --exclude=./desktopApp/build .",
            context.projectRoot,
        )
        if (result.exitCode != 0) throw FlikExecutionException("backup failed:\n${result.output}")
        context.recordBackup(fileName, destination)
    }

    companion object : PageElementParser {
        private const val PREFIX = "back up to file"
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val content = bulletOrLine(lines[index])
            if (!content.startsWith(PREFIX, ignoreCase = true)) return null
            val name = content.substring(PREFIX.length).trim().trim('`').trim()
            if (name.isEmpty()) throw FlikParseException("'$PREFIX' has no filename")
            return ParsedElement(BackUpCommand(name), index + 1)
        }
    }
}
