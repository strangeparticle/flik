package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.os.fileExists
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.os.readFileText
import com.strangeparticle.flik.parse.calloutFileName

/**
 * A bare bracketed page invocation, e.g. `[Build the signed, notarized DMG]`. Resolves
 * the name to a sibling `.flik.md` file (by normalization), parses it, and runs it as
 * a page — recursively, with the same execution context.
 */
class PageInvocation(val name: String) : PageElement {
    override fun execute(context: ExecutionContext) {
        val fileName = calloutFileName(name)
        val path = joinPath(context.currentPageDirectory, fileName)
        if (!fileExists(path)) throw FlikExecutionException("page \"$name\" -> $fileName not found")
        context.log("── $name")
        val page = parsePage(readFileText(path))
        runPage(page, pageId = path, pageDirectory = parentDirectoryOf(path), context = context)
    }

    companion object : PageElementParser {
        private val REGEX = Regex("\\[(.+)]")
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(PageInvocation(match.groupValues[1].trim()), index + 1)
        }
    }
}
