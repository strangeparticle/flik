package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.commands.AppendToFileCommand
import com.strangeparticle.flik.command.commands.CreateOrAppendToFileCommand
import com.strangeparticle.flik.command.commands.WriteToFileCommand
import com.strangeparticle.flik.os.fileExists
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.readFileText
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.writeFileText
import com.strangeparticle.flik.parse.FlikParseException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FileWriteCommandTest {
    private lateinit var projectRoot: String

    @BeforeTest
    fun createProjectRoot() {
        projectRoot = "/tmp/flik-file-write-test-${runShellCommand("echo $$").output.trim()}-${kotlin.random.Random.nextInt(0, 1_000_000)}"
        runShellCommand("mkdir -p ${com.strangeparticle.flik.os.singleQuote(projectRoot)}")
    }

    @AfterTest
    fun removeProjectRoot() {
        runShellCommand("rm -rf ${com.strangeparticle.flik.os.singleQuote(projectRoot)}")
    }

    private fun context(captures: Map<String, String> = emptyMap()): ExecutionContext {
        val context = ExecutionContext(
            projectRoot = projectRoot,
            backupsDirectory = joinPath(projectRoot, ".backups"),
            pages = emptyMap(),
            log = {},
        )
        for ((name, value) in captures) context.recordCapture(name, value)
        return context
    }

    private fun <T> withFrame(context: ExecutionContext, block: () -> T): T =
        context.withPage(joinPath(projectRoot, "page.flik.md"), projectRoot, block)

    // --- Parsing -----------------------------------------------------------

    @Test
    fun parsesWriteToFileWithAPathAndBlock() {
        val page = parsePage("# P\n\nWrite to file: `notes/out.txt`\n\n```text\nhello\n```")
        val command = assertIs<WriteToFileCommand>(page.elements.single())
        assertEquals("notes/out.txt", command.path)
        assertEquals("hello", command.content)
    }

    @Test
    fun parsesAppendToFile() {
        val page = parsePage("# P\n\nAppend to file: `log.txt`\n\n```text\nmore\n```")
        val command = assertIs<AppendToFileCommand>(page.elements.single())
        assertEquals("log.txt", command.path)
        assertEquals("more", command.content)
    }

    @Test
    fun parsesCreateOrAppendToFile() {
        val page = parsePage("# P\n\nCreate or append to file: `log.txt`\n\n```text\nmaybe\n```")
        val command = assertIs<CreateOrAppendToFileCommand>(page.elements.single())
        assertEquals("log.txt", command.path)
        assertEquals("maybe", command.content)
    }

    @Test
    fun writeToFileWithoutAFencedBlockIsAParseError() {
        assertFailsWith<FlikParseException> {
            parsePage("# P\n\nWrite to file: `out.txt`\n\nno block follows")
        }
    }

    // --- Write to file -----------------------------------------------------

    @Test
    fun writeToFileOverwritesAnExistingFile() {
        val target = joinPath(projectRoot, "out.txt")
        writeFileText(target, "old content that should be gone\n")
        val context = context()
        withFrame(context) { WriteToFileCommand("out.txt", "fresh line").execute(context) }
        assertEquals("fresh line\n", readFileText(target))
    }

    @Test
    fun writeToFileCreatesMissingParentDirectories() {
        val context = context()
        withFrame(context) { WriteToFileCommand("deep/nested/dir/out.txt", "created").execute(context) }
        assertEquals("created\n", readFileText(joinPath(projectRoot, "deep/nested/dir/out.txt")))
    }

    @Test
    fun writeToFileEndsContentWithASingleTrailingNewline() {
        val target = joinPath(projectRoot, "trailing.txt")
        val context = context()
        withFrame(context) { WriteToFileCommand("trailing.txt", "already ends\n").execute(context) }
        assertEquals("already ends\n", readFileText(target))
    }

    @Test
    fun writeToFileInterpolatesCapturesAndEnvAndProjectRoot() {
        // HOME is reliably set in the test environment; use it for the env.* substitution.
        val home = com.strangeparticle.flik.os.environmentVariable("HOME")
            ?: error("HOME must be set for this test")
        val target = joinPath(projectRoot, "rendered.txt")
        val context = context(mapOf("SERVICE_NAME" to "billing"))
        val body = "service=\${{ capture.SERVICE_NAME }}\nroot=\${{ project_root }}\nhome=\${{ env.HOME }}"
        withFrame(context) { WriteToFileCommand("rendered.txt", body).execute(context) }
        assertEquals("service=billing\nroot=$projectRoot\nhome=$home\n", readFileText(target))
    }

    // --- Append to file ----------------------------------------------------

    @Test
    fun appendToFileAppendsToTheEndOfAnExistingFile() {
        val target = joinPath(projectRoot, "log.txt")
        writeFileText(target, "first line\n")
        val context = context()
        withFrame(context) { AppendToFileCommand("log.txt", "second line").execute(context) }
        assertEquals("first line\nsecond line\n", readFileText(target))
    }

    @Test
    fun appendToFileErrorsWhenTheFileDoesNotExist() {
        val context = context()
        assertFailsWith<FlikExecutionException> {
            withFrame(context) { AppendToFileCommand("missing.txt", "x").execute(context) }
        }
        assertTrue(!fileExists(joinPath(projectRoot, "missing.txt")))
    }

    // --- Create or append to file ------------------------------------------

    @Test
    fun createOrAppendCreatesTheFileWhenAbsentIncludingParentDirectories() {
        val context = context()
        withFrame(context) { CreateOrAppendToFileCommand("new/area/log.txt", "born").execute(context) }
        assertEquals("born\n", readFileText(joinPath(projectRoot, "new/area/log.txt")))
    }

    @Test
    fun createOrAppendAppendsWhenTheFileIsPresent() {
        val target = joinPath(projectRoot, "log.txt")
        writeFileText(target, "head\n")
        val context = context()
        withFrame(context) { CreateOrAppendToFileCommand("log.txt", "tail").execute(context) }
        assertEquals("head\ntail\n", readFileText(target))
    }

    // --- Absolute paths ----------------------------------------------------

    @Test
    fun absolutePathsPassThroughUnchanged() {
        val target = joinPath(projectRoot, "absolute.txt")
        val context = context()
        withFrame(context) { WriteToFileCommand(target, "abs").execute(context) }
        assertEquals("abs\n", readFileText(target))
    }
}
