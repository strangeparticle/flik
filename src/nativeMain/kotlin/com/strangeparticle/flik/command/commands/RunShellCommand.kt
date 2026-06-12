package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.command.util.firstLine
import com.strangeparticle.flik.command.util.interpolate
import com.strangeparticle.flik.command.util.readFencedBlock
import com.strangeparticle.flik.os.runShellCommand

/**
 * Runs a shell command. Two forms:
 *  - `Run command:` + a fenced shell block (for multi-line commands)
 *  - `* run command: \`<cmd>\`` (single line)
 *
 * Fails the run on a non-zero exit. The command's combined output is remembered on the
 * context so a following `expect to see` can assert against it.
 */
class RunShellCommand(val command: String) : Command {
    override fun execute(context: ExecutionContext) {
        val resolved = interpolate(command, context.projectRoot)
        context.log("  run: ${firstLine(resolved)}")
        val result = runShellCommand(resolved, context.projectRoot)
        if (result.exitCode != 0) {
            throw FlikExecutionException(
                "command failed (exit ${result.exitCode}): ${firstLine(resolved)}\n${result.output}",
            )
        }
        context.lastCommandOutput = result.output
    }

    companion object : PageElementParser {
        private val INLINE_REGEX = Regex("run command: `(.+)`", RegexOption.IGNORE_CASE)
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            if (lines[index].trim().equals("Run command:", ignoreCase = true)) {
                val (block, next) = readFencedBlock(lines, index + 1)
                return ParsedElement(RunShellCommand(block.trim()), next)
            }
            val inline = INLINE_REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(RunShellCommand(inline.groupValues[1]), index + 1)
        }
    }
}
