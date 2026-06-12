package com.strangeparticle.flik.command

import com.strangeparticle.flik.os.runShellCommand

/** `Run command:` + a fenced shell block. Fails the run on a non-zero exit. */
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
    }

    companion object : PageElementParser {
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            if (lines[index].trim() != "Run command:") return null
            val (block, next) = readFencedBlock(lines, index + 1)
            return ParsedElement(RunShellCommand(block.trim()), next)
        }
    }
}
