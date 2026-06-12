package command

import os.joinPath
import os.parentDirectoryOf
import os.runShellCommand
import os.singleQuote

/** `Copy file from: <from> to: <to>` — `from` is relative to the stage document, `to` to the project root. */
class CopyFileStep(val from: String, val to: String) : StageStep {
    override fun execute(context: ExecutionContext) {
        val stageDirectory = context.stageDirectory ?: "."
        val source = if (from.startsWith("/")) from else joinPath(stageDirectory, from)
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

    companion object : StageStepParser {
        private val REGEX = Regex("\\* Copy file from: `(.+?)` to: `(.+?)`")
        override fun tryParse(lines: List<String>, index: Int): ParsedStageStep? {
            val match = REGEX.matchEntire(lines[index].trim()) ?: return null
            return ParsedStageStep(CopyFileStep(match.groupValues[1], match.groupValues[2]), index + 1)
        }
    }
}
