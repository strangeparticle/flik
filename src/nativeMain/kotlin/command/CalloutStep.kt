package command

import os.fileExists
import os.joinPath
import os.parentDirectoryOf
import os.readFileText
import parse.calloutFileName

/** A bare bracketed callout, e.g. `[Build the signed, notarized DMG]`; runs the named stage document. */
class CalloutStep(val name: String) : ProcedureStep {
    override fun execute(context: ExecutionContext) {
        val fileName = calloutFileName(name)
        val path = joinPath(context.entryDirectory, fileName)
        if (!fileExists(path)) throw FlikExecutionException("callout \"$name\" -> $fileName not found")
        context.log("── $name")

        val document = parseStageDocument(readFileText(path))
        val previousStageDirectory = context.stageDirectory
        context.stageDirectory = parentDirectoryOf(path)
        try {
            for (step in document.steps) {
                step.execute(context)
            }
        } finally {
            context.stageDirectory = previousStageDirectory
        }
    }

    companion object : ProcedureStepParser {
        private val REGEX = Regex("\\[(.+)]")
        override fun tryParse(bullet: String): ProcedureStep? {
            val match = REGEX.matchEntire(bullet) ?: return null
            return CalloutStep(match.groupValues[1].trim())
        }
    }
}
