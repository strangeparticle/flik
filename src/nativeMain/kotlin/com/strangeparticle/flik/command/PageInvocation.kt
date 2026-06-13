package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.parse.calloutFileName

/**
 * A bare bracketed page invocation, e.g. `[Build the signed, notarized DMG]`. Resolves
 * the name to a sibling `.flik.md` file (by normalization) and runs that page, looked up
 * from the compiled program — every page was parsed once during linking, so callouts
 * never re-read or re-parse.
 */
class PageInvocation(val name: String) : PageElement {
    override fun execute(context: ExecutionContext) {
        val fileName = calloutFileName(name)
        val path = joinPath(context.currentPageDirectory, fileName)
        val page = context.pages[path]
            ?: throw FlikExecutionException("page \"$name\" -> $fileName was not in the compiled program")
        context.log("── $name")
        runPage(page, pageId = path, pageDirectory = parentDirectoryOf(path), context = context)
    }

    override fun referencedPageInvocations(): List<PageInvocation> = listOf(this)

    companion object : PageElementParser {
        private val REGEX = Regex("\\[(.+)]")
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(PageInvocation(match.groupValues[1].trim()), index + 1)
        }
    }
}
