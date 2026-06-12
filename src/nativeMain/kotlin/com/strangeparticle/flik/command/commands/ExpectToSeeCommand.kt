package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.bulletOrLine

/**
 * `expect to see \`<text>\`` — assert that the most recent command's output contains
 * <text> (a literal substring). Pairs with a preceding `run command`, which remembers
 * its output.
 */
class ExpectToSeeCommand(val expected: String) : Command {
    override fun execute(context: ExecutionContext) {
        val output = context.lastCommandOutput
            ?: throw FlikExecutionException("'expect to see' used before any command produced output")
        context.log("  expect to see: $expected")
        if (!output.contains(expected)) {
            throw FlikExecutionException(
                "expected to see \"$expected\" in the last command's output, but it was not present",
            )
        }
    }

    companion object : PageElementParser {
        private val REGEX = Regex("expect to see `(.+)`", RegexOption.IGNORE_CASE)
        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return ParsedElement(ExpectToSeeCommand(match.groupValues[1]), index + 1)
        }
    }
}
