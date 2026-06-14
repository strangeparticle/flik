package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.parseFileWriteElement
import com.strangeparticle.flik.command.util.withTrailingNewline
import com.strangeparticle.flik.os.makeDirectoriesFor
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.os.writeFileText

/**
 * `Write to file: \`path\`` + a fenced block — overwrites the file with the block body,
 * creating it (and any missing parent directories) if absent. The body is interpolated like
 * a shell block, and a single trailing newline is ensured to match heredoc behavior.
 */
class WriteToFileCommand(val path: String, val content: String) : Command {
    override fun execute(context: ExecutionContext) {
        val resolvedPath = context.resolveAgainstProjectRoot(path)
        context.log("  write file: $path")
        makeDirectoriesFor(parentDirectoryOf(resolvedPath))
        writeFileText(resolvedPath, withTrailingNewline(context.interpolate(content)))
    }

    companion object : PageElementParser {
        private const val LABEL = "Write to file"
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val parsed = parseFileWriteElement(LABEL, lines, index) ?: return null
            return ParsedElement(WriteToFileCommand(parsed.path, parsed.content), parsed.nextIndex)
        }
    }
}
