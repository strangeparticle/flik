package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Check
import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.firstLine
import com.strangeparticle.flik.command.util.interpolate
import com.strangeparticle.flik.os.runShellCommand

/**
 * `Run these checks in parallel:` + a bullet list of checks. (Checks currently run
 * sequentially; the surface is designed for parallel execution, which is coming.)
 */
class RunChecksCommand(val checks: List<Check>) : Command {
    override fun execute(context: ExecutionContext) {
        context.log("  ${checks.size} checks:")
        for (check in checks) {
            val command = interpolate(check.command, context.projectRoot)
            val result = runShellCommand(command, context.projectRoot)
            if (result.exitCode != 0) {
                throw FlikExecutionException("check failed: ${firstLine(check.description)}\n${result.output}")
            }
            val expected = check.expectContains
            if (expected != null && !result.output.contains(interpolate(expected, context.projectRoot))) {
                throw FlikExecutionException(
                    "check output did not contain \"$expected\": ${firstLine(check.description)}",
                )
            }
            context.log("    ✓ ${firstLine(check.description)}")
        }
    }

    companion object : PageElementParser {
        private val COMMAND_REGEX = Regex("run `([^`]+)`")
        private val CONTAINS_REGEX = Regex("contain `([^`]+)`")
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            if (lines[index].trim() != "Run these checks in parallel:") return null
            val checks = mutableListOf<Check>()
            var j = index + 1
            while (j < lines.size) {
                val trimmed = lines[j].trim()
                if (trimmed.startsWith("* ")) {
                    val command = COMMAND_REGEX.find(trimmed)?.groupValues?.get(1)
                    if (command != null) {
                        val contains = CONTAINS_REGEX.find(trimmed)?.groupValues?.get(1)
                        checks.add(Check(trimmed.removePrefix("* ").trim(), command, contains))
                    }
                    j++
                } else if (trimmed.isEmpty() && checks.isEmpty()) {
                    j++
                } else {
                    break
                }
            }
            return ParsedElement(RunChecksCommand(checks), j)
        }
    }
}
