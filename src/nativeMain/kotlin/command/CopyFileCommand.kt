package command

import os.joinPath
import os.parentDirectoryOf
import os.runShellCommand
import os.singleQuote

/** `Copy file from: <from> to: <to>` — `from` is relative to the current page, `to` to the project root. */
class CopyFileCommand(val from: String, val to: String) : Command {
    override fun execute(context: ExecutionContext) {
        val source = if (from.startsWith("/")) from else joinPath(context.currentPageDirectory, from)
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
