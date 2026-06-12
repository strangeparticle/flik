package command

import os.fileExists
import os.readFileText
import os.writeFileText
import parse.FlikParseException

/** `In file: <file>` / `Find this section:` / `Replace it with:` — a single in-place edit. */
class EditInPlaceCommand(val file: String, val find: String, val replace: String) : Command {
    override fun execute(context: ExecutionContext) {
        val path = context.resolveAgainstProjectRoot(file)
        context.log("  edit: $file")
        if (!fileExists(path)) throw FlikExecutionException("file to edit not found: $path")
        val original = readFileText(path)
        val findBlock = interpolate(find, context.projectRoot)
        val replaceBlock = interpolate(replace, context.projectRoot)
        if (!original.contains(findBlock)) {
            throw FlikExecutionException("find block not found in $file")
        }
        writeFileText(path, original.replaceFirst(findBlock, replaceBlock))
    }

    companion object : PageElementParser {
        private val REGEX = Regex("In file: `(.+?)`")
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            val file = match.groupValues[1]

            val findLabel = indexOfTrimmed(lines, index + 1, "Find this section:")
            if (findLabel == -1) throw FlikParseException("expected 'Find this section:' after In file: `$file`")
            val (findBlock, afterFind) = readFencedBlock(lines, findLabel + 1)

            val replaceLabel = indexOfTrimmed(lines, afterFind, "Replace it with:")
            if (replaceLabel == -1) throw FlikParseException("expected 'Replace it with:' for In file: `$file`")
            val (replaceBlock, afterReplace) = readFencedBlock(lines, replaceLabel + 1)

            return ParsedElement(EditInPlaceCommand(file, findBlock, replaceBlock), afterReplace)
        }
    }
}
