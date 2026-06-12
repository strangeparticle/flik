package command

import os.fileExists

/** `Expect file to exist: <path>`. */
class ExpectFileCommand(val path: String) : Command {
    override fun execute(context: ExecutionContext) {
        val resolved = context.resolveAgainstProjectRoot(path)
        context.log("  expect file: $resolved")
        if (!fileExists(resolved)) throw FlikExecutionException("expected file to exist: $resolved")
    }

    companion object : PageElementParser {
        private val REGEX = Regex("Expect file to exist: `(.+?)`")
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(ExpectFileCommand(match.groupValues[1]), index + 1)
        }
    }
}
