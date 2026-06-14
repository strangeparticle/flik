package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.os.fileExists
import com.strangeparticle.flik.os.readFileText

/**
 * `Expect file to be empty: <path>`.
 *
 * A shared-resource guard: it passes when nobody is using the file and fails loudly
 * when someone is. "Empty" means absent, zero bytes, or whitespace-only (`isBlank()`).
 * On failure the message names the resolved path and previews the current contents.
 */
class ExpectFileEmptyCommand(val path: String) : Command {
    override fun execute(context: ExecutionContext) {
        val resolved = context.resolveAgainstProjectRoot(path)
        context.log("  expect file empty: $resolved")
        if (!fileExists(resolved)) return
        val contents = readFileText(resolved)
        if (contents.isBlank()) return
        throw FlikExecutionException(
            "expected file to be empty: $resolved\ncurrent contents:\n${previewOf(contents)}"
        )
    }

    companion object : PageElementParser {
        private val REGEX = Regex("Expect file to be empty: `(.+?)`")

        /** Number of leading content lines shown in a failure preview before truncation. */
        private const val PREVIEW_LINE_LIMIT = 10

        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(ExpectFileEmptyCommand(match.groupValues[1]), index + 1)
        }

        /**
         * The first [PREVIEW_LINE_LIMIT] lines of [contents], with a "…(truncated)" marker
         * appended on its own line when the file has more lines than that.
         */
        private fun previewOf(contents: String): String {
            val allLines = contents.lines()
            if (allLines.size <= PREVIEW_LINE_LIMIT) {
                return contents
            }
            val shownLines = allLines.take(PREVIEW_LINE_LIMIT)
            return shownLines.joinToString("\n") + "\n…(truncated)"
        }
    }
}
