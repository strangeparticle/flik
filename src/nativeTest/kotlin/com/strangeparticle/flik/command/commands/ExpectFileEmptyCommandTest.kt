package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.Page
import com.strangeparticle.flik.command.ParsedElement
import com.strangeparticle.flik.command.parseOneElement
import com.strangeparticle.flik.os.environmentVariable
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.writeFileText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExpectFileEmptyCommandTest {
    private val temporaryDirectory: String = environmentVariable("TMPDIR") ?: "/tmp"

    private fun contextRootedAt(projectRoot: String): ExecutionContext =
        ExecutionContext(
            projectRoot = projectRoot,
            backupsDirectory = joinPath(projectRoot, ".backups"),
            pages = emptyMap<String, Page>(),
            log = {},
        )

    private fun uniquePath(name: String): String =
        joinPath(temporaryDirectory, "flik-expect-empty-test-${name}-${nextSuffix()}")

    private fun nextSuffix(): String {
        suffixCounter += 1
        return suffixCounter.toString()
    }

    // --- execution: passing cases ---

    @Test
    fun passesWhenFileIsAbsent() {
        val absentPath = uniquePath("absent")
        val context = contextRootedAt(temporaryDirectory)
        // No file written, so the path does not exist; this must not throw.
        ExpectFileEmptyCommand(absentPath).execute(context)
    }

    @Test
    fun passesWhenFileIsZeroBytes() {
        val zeroBytePath = uniquePath("zero")
        writeFileText(zeroBytePath, "")
        val context = contextRootedAt(temporaryDirectory)
        ExpectFileEmptyCommand(zeroBytePath).execute(context)
    }

    @Test
    fun passesWhenFileIsWhitespaceOnly() {
        val whitespacePath = uniquePath("whitespace")
        writeFileText(whitespacePath, "  \n\t\n   \n")
        val context = contextRootedAt(temporaryDirectory)
        ExpectFileEmptyCommand(whitespacePath).execute(context)
    }

    // --- execution: failing cases ---

    @Test
    fun failsWhenFileHasASingleNonEmptyLineAndShowsThePreview() {
        val occupiedPath = uniquePath("single-line")
        writeFileText(occupiedPath, "in use by deploy-12345\n")
        val context = contextRootedAt(temporaryDirectory)

        val failure = assertFailsWith<FlikExecutionException> {
            ExpectFileEmptyCommand(occupiedPath).execute(context)
        }
        val message = failure.message ?: ""
        assertTrue(message.contains(occupiedPath), "message should name the resolved path: $message")
        assertTrue(message.contains("in use by deploy-12345"), "message should preview the contents: $message")
        assertTrue(!message.contains("…(truncated)"), "single line must not be truncated: $message")
    }

    @Test
    fun failsWhenFileExceedsTenLinesAndTruncatesThePreview() {
        val occupiedPath = uniquePath("many-lines")
        val twelveLines = (1..12).joinToString("\n") { "line-$it" }
        writeFileText(occupiedPath, twelveLines)
        val context = contextRootedAt(temporaryDirectory)

        val failure = assertFailsWith<FlikExecutionException> {
            ExpectFileEmptyCommand(occupiedPath).execute(context)
        }
        val message = failure.message ?: ""
        assertTrue(message.contains("line-1\n"), "first line should be shown: $message")
        assertTrue(message.contains("line-10"), "tenth line should be shown: $message")
        assertTrue(!message.contains("line-11"), "eleventh line should be truncated away: $message")
        assertTrue(!message.contains("line-12"), "twelfth line should be truncated away: $message")
        assertTrue(message.contains("…(truncated)"), "over-limit file must show the truncation marker: $message")
    }

    // --- parsing ---

    @Test
    fun parsesTheBacktickedPathFromABulletOrLine() {
        val lines = listOf("Expect file to be empty: `${'$'}{{ project_root }}/locks/deploy.lock`")
        val parsed: ParsedElement? = parseOneElement(lines, 0)
        val command = assertIs<ExpectFileEmptyCommand>(parsed?.element)
        assertEquals("${'$'}{{ project_root }}/locks/deploy.lock", command.path)
        assertEquals(1, parsed?.nextIndex)
    }

    private companion object {
        private var suffixCounter = 0
    }
}
