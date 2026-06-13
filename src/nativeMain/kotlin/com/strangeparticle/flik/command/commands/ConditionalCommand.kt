package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.PageElement
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.PageInvocation
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.parseOneElement
import com.strangeparticle.flik.parse.FlikParseException

/**
 * A block-level conditional:
 *
 *     If <predicate>:
 *     <one element>
 *     Otherwise:
 *     <one element>
 *
 * Runs [thenBranch] when [predicate] holds, otherwise [elseBranch] (a no-op when there is
 * no `Otherwise:`). Each branch is exactly one element — a command or a page invocation —
 * so a multi-step branch is expressed as its own page. This is the general form; the inline
 * `If the command fails:` modifier on a single run command stays as the ergonomic shorthand.
 */
class ConditionalCommand(
    val predicate: Predicate,
    val thenBranch: PageElement,
    val elseBranch: PageElement?,
) : Command {
    override fun execute(context: ExecutionContext) {
        if (predicate.evaluate(context)) {
            context.log("  if (condition holds): running this branch")
            thenBranch.execute(context)
        } else if (elseBranch != null) {
            context.log("  if (condition fails): running the otherwise branch")
            elseBranch.execute(context)
        } else {
            context.log("  if (condition fails): no otherwise branch — skipping")
        }
    }

    override fun referencedPageInvocations(): List<PageInvocation> =
        thenBranch.referencedPageInvocations() + (elseBranch?.referencedPageInvocations() ?: emptyList())

    companion object : PageElementParser {
        private val IF_REGEX = Regex("If (.+):", RegexOption.IGNORE_CASE)
        private const val OTHERWISE = "Otherwise:"

        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val match = IF_REGEX.matchEntire(lines[index].trim()) ?: return null
            // `If the command fails:` (and any other unrecognized condition) is not ours — a
            // failed predicate parse means we leave the line for another parser/modifier.
            val predicate = Predicate.parse(match.groupValues[1]) ?: return null

            val thenBranch = parseBranch(lines, index + 1)
                ?: throw FlikParseException("expected a step after: If ${match.groupValues[1]}:")

            var afterThen = skipBlank(lines, thenBranch.nextIndex)
            var elseBranch: PageElement? = null
            if (afterThen < lines.size && lines[afterThen].trim().equals(OTHERWISE, ignoreCase = true)) {
                val parsedElse = parseBranch(lines, afterThen + 1)
                    ?: throw FlikParseException("expected a step after Otherwise:")
                elseBranch = parsedElse.element
                afterThen = parsedElse.nextIndex
            }
            return ParsedElement(ConditionalCommand(predicate, thenBranch.element, elseBranch), afterThen)
        }

        private fun parseBranch(lines: List<String>, from: Int): ParsedElement? {
            val start = skipBlank(lines, from)
            if (start >= lines.size) return null
            return parseOneElement(lines, start)
        }

        private fun skipBlank(lines: List<String>, from: Int): Int {
            var i = from
            while (i < lines.size && lines[i].isBlank()) i++
            return i
        }
    }
}
