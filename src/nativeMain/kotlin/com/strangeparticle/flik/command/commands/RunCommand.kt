package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.command.util.firstLine
import com.strangeparticle.flik.command.util.parseCaptureModifier
import com.strangeparticle.flik.command.util.parseLabeledCommandBlock
import com.strangeparticle.flik.command.util.readFencedBlock
import com.strangeparticle.flik.os.runShellCommand

/**
 * Runs a shell command, plus any modifiers that operate on *its* result. The command
 * is written as `Run command:` + a fenced shell block, or single-line `* run command:
 * \`cmd\``, and may be directly followed by:
 *  - `* capture output as: \`NAME\`` — bind its (trimmed) output to `${'$'}{{ capture.NAME }}`,
 *  - `* expect to see: \`text\`` — assert its output contains `text` (repeatable),
 *  - `If the command fails:` + a fenced shell block — run this fallback if the command
 *    exits non-zero, instead of failing the run.
 *
 * Modifiers attach to the command structurally — there is no ambient "previous command".
 */
class RunCommand(
    val command: String,
    val captureAs: String?,
    val expectToSee: List<String>,
    val fallbackCommand: String?,
) : Command {
    override fun execute(context: ExecutionContext) {
        val resolved = context.interpolate(command)
        context.log("  run: ${firstLine(resolved)}")
        var result = runShellCommand(resolved, context.projectRoot)

        if (result.exitCode != 0) {
            if (fallbackCommand == null) {
                throw FlikExecutionException(
                    "command failed (exit ${result.exitCode}): ${firstLine(resolved)}\n${result.output}",
                )
            }
            val resolvedFallback = context.interpolate(fallbackCommand)
            context.log("  command failed — running fallback: ${firstLine(resolvedFallback)}")
            result = runShellCommand(resolvedFallback, context.projectRoot)
            if (result.exitCode != 0) {
                throw FlikExecutionException(
                    "command and fallback both failed (exit ${result.exitCode}): ${firstLine(resolvedFallback)}\n${result.output}",
                )
            }
        }

        for (expected in expectToSee) {
            val needle = context.interpolate(expected)
            if (!result.output.contains(needle)) {
                throw FlikExecutionException(
                    "expected to see \"$needle\" in the command's output, but it was not present",
                )
            }
        }

        captureAs?.let { name -> context.recordCapture(name, result.output.trim()) }
    }

    companion object : PageElementParser {
        private val INLINE_REGEX = Regex("run command: `(.+)`", RegexOption.IGNORE_CASE)
        private val EXPECT_REGEX = Regex("expect to see:?\\s*`(.+)`", RegexOption.IGNORE_CASE)
        private const val FALLBACK_LABEL = "If the command fails:"

        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val (command, afterCommand) = parsePrimaryCommand(lines, index) ?: return null

            var i = afterCommand
            var captureAs: String? = null
            val expectToSee = mutableListOf<String>()
            var fallbackCommand: String? = null

            // Consume modifiers that directly follow the command (blank lines tolerated).
            while (i < lines.size) {
                val raw = lines[i]
                if (raw.isBlank()) {
                    i++
                    continue
                }
                val content = bulletOrLine(raw)
                val capture = parseCaptureModifier(content)
                val expect = EXPECT_REGEX.matchEntire(content)
                when {
                    capture != null -> {
                        captureAs = capture
                        i++
                    }
                    expect != null -> {
                        expectToSee += expect.groupValues[1]
                        i++
                    }
                    content.equals(FALLBACK_LABEL, ignoreCase = true) -> {
                        val (block, next) = readFencedBlock(lines, i + 1)
                        fallbackCommand = block.trim()
                        i = next
                    }
                    else -> break
                }
            }

            return ParsedElement(RunCommand(command, captureAs, expectToSee, fallbackCommand), i)
        }

        private fun parsePrimaryCommand(lines: List<String>, index: Int): Pair<String, Int>? {
            parseLabeledCommandBlock(lines, index, "Run command:")?.let { return it }
            val inline = INLINE_REGEX.matchEntire(bulletOrLine(lines[index])) ?: return null
            return inline.groupValues[1] to (index + 1)
        }
    }
}
