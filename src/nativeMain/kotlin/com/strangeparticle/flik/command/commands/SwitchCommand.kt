package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElement
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.PageInvocation
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.parseOneElement
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.parse.FlikParseException

/**
 * A block-level switch:
 *
 *     Depending on `<selector>`:
 *     * `prod`: [Deploy to production]
 *     * `dev`: [Deploy to dev]
 *     * otherwise: [Choose an environment]
 *
 * Interpolates [selector], matches it (exact, trimmed, case-sensitive) against the branch
 * literals, and runs that branch's element. With no match and no `otherwise`, the run fails
 * clearly. Each branch element is a single one-line element — typically a page invocation or
 * a `* run command: \`cmd\`` — so complex branches live in their own pages.
 */
class SwitchCommand(
    val selector: String,
    val branches: List<Branch>,
    val otherwise: PageElement?,
) : Command {
    class Branch(val literal: String, val element: PageElement)

    override fun execute(context: ExecutionContext) {
        val value = context.interpolate(selector).trim()
        context.log("  depending on \"$value\"")
        val chosen = branches.firstOrNull { it.literal == value }?.element
            ?: otherwise
            ?: throw FlikExecutionException("no branch matched \"$value\" and there is no 'otherwise'")
        chosen.execute(context)
    }

    override fun referencedPageInvocations(): List<PageInvocation> =
        (branches.map { it.element } + listOfNotNull(otherwise)).flatMap { it.referencedPageInvocations() }

    companion object : PageElementParser {
        private val SELECTOR_REGEX = Regex("Depending on `(.+?)`:", RegexOption.IGNORE_CASE)
        private val BRANCH_REGEX = Regex("`(.+?)`:\\s*(.+)")
        private val OTHERWISE_REGEX = Regex("otherwise:\\s*(.+)", RegexOption.IGNORE_CASE)

        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val selectorMatch = SELECTOR_REGEX.matchEntire(lines[index].trim()) ?: return null
            val selector = selectorMatch.groupValues[1]

            val branches = mutableListOf<Branch>()
            var otherwise: PageElement? = null
            var i = index + 1
            while (i < lines.size) {
                val raw = lines[i]
                if (raw.isBlank()) {
                    i++
                    continue
                }
                if (!raw.trimStart().startsWith("* ")) break
                val content = bulletOrLine(raw)
                val otherwiseMatch = OTHERWISE_REGEX.matchEntire(content)
                val branchMatch = BRANCH_REGEX.matchEntire(content)
                when {
                    otherwiseMatch != null -> {
                        otherwise = parseBranchElement(otherwiseMatch.groupValues[1])
                        i++
                    }
                    branchMatch != null -> {
                        branches += Branch(branchMatch.groupValues[1], parseBranchElement(branchMatch.groupValues[2]))
                        i++
                    }
                    else -> break
                }
            }
            if (branches.isEmpty() && otherwise == null) {
                throw FlikParseException("'Depending on `$selector`:' has no branches")
            }
            return ParsedElement(SwitchCommand(selector, branches, otherwise), i)
        }

        private fun parseBranchElement(text: String): PageElement {
            val parsed = parseOneElement(listOf(text.trim()), 0)
                ?: throw FlikParseException("unrecognized switch branch: \"$text\"")
            return parsed.element
        }
    }
}
