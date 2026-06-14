package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.Command
import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.PageElementParser
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.util.PollAttempt
import com.strangeparticle.flik.command.util.PollOutcome
import com.strangeparticle.flik.command.util.bulletOrLine
import com.strangeparticle.flik.command.util.containsIgnoringWhitespace
import com.strangeparticle.flik.command.util.firstLine
import com.strangeparticle.flik.command.util.parseCaptureModifier
import com.strangeparticle.flik.command.util.parseDurationMillis
import com.strangeparticle.flik.command.util.parseLabeledCommandBlock
import com.strangeparticle.flik.command.util.pollUntilMatch
import com.strangeparticle.flik.command.util.readFencedBlock
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.sleepMillis
import com.strangeparticle.flik.parse.FlikParseException
import kotlin.time.TimeSource

private const val DEFAULT_INTERVAL_MILLIS = 5_000L

/**
 * Repeatedly runs a shell command until its output contains an author-provided example
 * (compared with all whitespace removed), or a required timeout elapses:
 *
 *     Poll by running:
 *     ```shell
 *     curl -sf https://…/status
 *     ```
 *     Until the output contains:
 *     ```json
 *     "state": "success"
 *     ```
 *     * time out after: `10m`
 *     * check every: `15s`
 *     * capture output as: `NAME`
 *
 * Flik runs the very command a human would run by hand, so the page stays followable
 * without Flik. Exit codes are not part of the match — only the output is, compared as a
 * whitespace-insensitive *substring* (so the example should be the smallest distinctive
 * sub-portion, not a full braced object that surrounding fields would split). The fenced
 * command block and the `capture output as:` modifier are parsed with the same shared
 * helpers as [RunCommand].
 */
class PollForStatusCommand(
    val command: String,
    val expectedOutput: String,
    val timeoutMillis: Long,
    val intervalMillis: Long,
    val captureAs: String?,
) : Command {
    override fun execute(context: ExecutionContext) {
        val resolvedCommand = context.interpolate(command)
        val resolvedExpected = context.interpolate(expectedOutput)
        context.log("  poll: ${firstLine(resolvedCommand)}")
        val started = TimeSource.Monotonic.markNow()

        val outcome = pollUntilMatch(
            intervalMillis = intervalMillis,
            timeoutMillis = timeoutMillis,
            runAttempt = {
                val result = runShellCommand(resolvedCommand, context.projectRoot)
                PollAttempt(result.exitCode, result.output)
            },
            matches = { output -> containsIgnoringWhitespace(output, resolvedExpected) },
            elapsedMillis = { started.elapsedNow().inWholeMilliseconds },
            sleep = ::sleepMillis,
        )

        when (outcome) {
            is PollOutcome.Matched -> {
                context.log("  poll matched after ${outcome.attempts} attempt(s)")
                captureAs?.let { name -> context.recordCapture(name, outcome.output.trim()) }
            }
            is PollOutcome.TimedOut -> throw FlikExecutionException(timeoutMessage(resolvedCommand, outcome))
        }
    }

    companion object : PageElementParser {
        private const val POLL_LABEL = "Poll by running:"
        private const val UNTIL_LABEL = "Until the output contains:"
        private val TIMEOUT_REGEX = Regex("time out after:?\\s*`(.+)`", RegexOption.IGNORE_CASE)
        private val INTERVAL_REGEX = Regex("check every:?\\s*`(.+)`", RegexOption.IGNORE_CASE)

        override fun tryParse(lines: List<String>, index: Int): ParsedElement? {
            val (command, afterCommand) = parseLabeledCommandBlock(lines, index, POLL_LABEL) ?: return null

            var i = skipBlank(lines, afterCommand)
            if (i >= lines.size || !lines[i].trim().equals(UNTIL_LABEL, ignoreCase = true)) {
                throw FlikParseException("expected \"$UNTIL_LABEL\" after \"$POLL_LABEL\"")
            }
            val (expected, afterExpected) = readFencedBlock(lines, i + 1)

            i = afterExpected
            var timeoutMillis: Long? = null
            var intervalMillis: Long? = null
            var captureAs: String? = null

            // Consume modifiers that follow the until-block (blank lines tolerated).
            while (i < lines.size) {
                val raw = lines[i]
                if (raw.isBlank()) {
                    i++
                    continue
                }
                val content = bulletOrLine(raw)
                val timeout = TIMEOUT_REGEX.matchEntire(content)
                val interval = INTERVAL_REGEX.matchEntire(content)
                val capture = parseCaptureModifier(content)
                when {
                    timeout != null -> {
                        timeoutMillis = parseDurationMillis(timeout.groupValues[1])
                        i++
                    }
                    interval != null -> {
                        intervalMillis = parseDurationMillis(interval.groupValues[1])
                        i++
                    }
                    capture != null -> {
                        captureAs = capture
                        i++
                    }
                    else -> break
                }
            }

            if (timeoutMillis == null) {
                throw FlikParseException("\"$POLL_LABEL\" requires a `time out after: \\`<duration>\\`` modifier")
            }

            return ParsedElement(
                PollForStatusCommand(
                    command = command,
                    expectedOutput = expected.trim(),
                    timeoutMillis = timeoutMillis,
                    intervalMillis = intervalMillis ?: DEFAULT_INTERVAL_MILLIS,
                    captureAs = captureAs,
                ),
                i,
            )
        }

        private fun skipBlank(lines: List<String>, from: Int): Int {
            var i = from
            while (i < lines.size && lines[i].isBlank()) i++
            return i
        }
    }
}

/** A clear timeout failure — the command, how long it ran, and the last attempt's output. */
private fun timeoutMessage(command: String, outcome: PollOutcome.TimedOut): String {
    val seconds = outcome.elapsedMillis / 1_000
    val lastAttempt = outcome.lastAttempt
    val lastOutput = lastAttempt?.output?.trim().orEmpty()
    val snippet = if (lastOutput.length > 1_000) lastOutput.take(1_000) + "…" else lastOutput
    return buildString {
        append("poll timed out after ${seconds}s and ${outcome.attempts} attempt(s) ")
        append("without the output containing the expected example\n")
        append("  command: ${firstLine(command)}\n")
        if (lastAttempt != null) {
            append("  last exit status: ${lastAttempt.exitCode}\n")
        }
        append("  last output:\n$snippet")
    }
}
