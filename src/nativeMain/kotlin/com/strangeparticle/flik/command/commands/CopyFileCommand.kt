package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.singleQuote

/** `Copy file from: <from> to: <to>` — `from` is relative to the current page, `to` to the project root. */
class CopyFileCommand(val from: String, val to: String) : Command {
    override fun execute(context: ExecutionContext) {
        val source = context.resolveAgainstCurrentPageDirectory(from)
        val destination = context.resolveAgainstProjectRoot(to)
        val destinationDirectory = parentDirectoryOf(destination)
        context.log("  copy $from -> $to")
        val result = runShellCommand(
            "mkdir -p ${singleQuote(destinationDirectory)} && " +
                "cp ${singleQuote(source)} ${singleQuote(destination)}",
        )
        if (result.exitCode != 0) {
            throw FlikExecutionException("copy failed ($from -> $to):\n${result.output}")
        }
    }

    companion object : PageElementParser {
        private val REGEX = Regex("Copy file from: `(.+?)` to: `(.+?)`")
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(CopyFileCommand(match.groupValues[1], match.groupValues[2]), index + 1)
        }
    }
}
