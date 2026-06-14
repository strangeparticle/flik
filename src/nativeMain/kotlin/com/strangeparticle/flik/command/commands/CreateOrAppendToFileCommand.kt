package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.parseFileWriteElement
import com.strangeparticle.flik.command.util.withTrailingNewline
import com.strangeparticle.flik.os.appendFileText
import com.strangeparticle.flik.os.fileExists
import com.strangeparticle.flik.os.makeDirectoriesFor
import com.strangeparticle.flik.os.parentDirectoryOf

/**
 * `Create or append to file: \`path\`` + a fenced block — appends the block body to an
 * existing file, or creates the file (and any missing parent directories) when it is absent.
 * The body is interpolated like a shell block, and a single trailing newline is ensured to
 * match heredoc behavior.
 */
class CreateOrAppendToFileCommand(val path: String, val content: String) : Command {
    override fun execute(context: ExecutionContext) {
        val resolvedPath = context.resolveAgainstProjectRoot(path)
        context.log("  create or append to file: $path")
        if (!fileExists(resolvedPath)) {
            makeDirectoriesFor(parentDirectoryOf(resolvedPath))
        }
        appendFileText(resolvedPath, withTrailingNewline(context.interpolate(content)))
    }

    companion object : PageElementParser {
        private const val LABEL = "Create or append to file"
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val parsed = parseFileWriteElement(LABEL, lines, index) ?: return null
            return ParsedElement(CreateOrAppendToFileCommand(parsed.path, parsed.content), parsed.nextIndex)
        }
    }
}
