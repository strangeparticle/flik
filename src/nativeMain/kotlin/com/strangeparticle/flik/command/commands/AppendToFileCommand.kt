package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.parseFileWriteElement
import com.strangeparticle.flik.command.util.withTrailingNewline
import com.strangeparticle.flik.os.appendFileText
import com.strangeparticle.flik.os.fileExists

/**
 * `Append to file: \`path\`` + a fenced block — appends the block body to the end of an
 * existing file. Errors if the file does not already exist; it does not create it (use
 * `Create or append to file:` for create-if-absent). The body is interpolated like a shell
 * block, and a single trailing newline is ensured to match heredoc behavior.
 */
class AppendToFileCommand(val path: String, val content: String) : Command {
    override fun execute(context: ExecutionContext) {
        val resolvedPath = context.resolveAgainstProjectRoot(path)
        context.log("  append to file: $path")
        if (!fileExists(resolvedPath)) {
            throw FlikExecutionException("file to append to not found: $resolvedPath")
        }
        appendFileText(resolvedPath, withTrailingNewline(context.interpolate(content)))
    }

    companion object : PageElementParser {
        private const val LABEL = "Append to file"
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val parsed = parseFileWriteElement(LABEL, lines, index) ?: return null
            return ParsedElement(AppendToFileCommand(parsed.path, parsed.content), parsed.nextIndex)
        }
    }
}
