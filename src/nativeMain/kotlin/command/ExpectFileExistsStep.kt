package command

import os.fileExists

/** `Expect file to exist: <path>`. */
class ExpectFileExistsStep(val path: String) : StageStep {
    override fun execute(context: ExecutionContext) {
        val resolved = context.resolveAgainstProjectRoot(path)
        context.log("  expect file: $resolved")
        if (!fileExists(resolved)) throw FlikExecutionException("expected file to exist: $resolved")
    }

    companion object : StageStepParser {
        private val REGEX = Regex("Expect file to exist: `(.+?)`")
        override fun tryParse(lines: List<String>, index: Int): ParsedStageStep? {
            val match = REGEX.matchEntire(lines[index].trim()) ?: return null
            return ParsedStageStep(ExpectFileExistsStep(match.groupValues[1]), index + 1)
        }
    }
}
