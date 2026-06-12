package com.strangeparticle.flik.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import com.strangeparticle.flik.command.commands.BackUpCommand
import com.strangeparticle.flik.command.commands.EditInPlaceCommand
import com.strangeparticle.flik.command.commands.ExpectFileCommand
import com.strangeparticle.flik.command.commands.ExpectToSeeCommand
import com.strangeparticle.flik.command.commands.RestoreCommand
import com.strangeparticle.flik.command.commands.RunShellCommand
import com.strangeparticle.flik.parse.FlikParseException

private val SAMPLE = """
    # Sample Page

    # Pre-requisites
    * flik version: `0.1`
    * shell command: `tar`

    Environment Variables:
    * TOKEN

    Shell Commands:
    * cp

    # Body

    * back up to file backup.tar.gz
    * [Do a thing]

    In file: `config.txt`

    Find this section:

    ```text
    a
    ```

    Replace it with:

    ```text
    b
    ```

    Run command:

    ```shell
    echo hi
    ```

    Expect file to exist: `${'$'}{{ project_root }}/out.txt`

    * restore the repository
""".trimIndent()

class PageParserTest {
    private val page = parsePage(SAMPLE)

    @Test
    fun parsesPrerequisitesInBothInlineAndGroupedForms() {
        assertEquals("Sample Page", page.title)
        assertEquals("0.1", page.flikVersion)               // inline `* flik version: ...`
        assertEquals(listOf("TOKEN"), page.requiredEnvironmentVariables)
        // grouped (Shell Commands: cp) plus inline (* shell command: tar)
        assertEquals(listOf("cp", "tar"), page.requiredShellCommands)
    }

    @Test
    fun parsesBodyElementsInDocumentOrder() {
        assertEquals(6, page.elements.size)
        assertIs<BackUpCommand>(page.elements[0])
        assertIs<PageInvocation>(page.elements[1])
        assertIs<EditInPlaceCommand>(page.elements[2])
        assertIs<RunShellCommand>(page.elements[3])
        assertIs<ExpectFileCommand>(page.elements[4])
        assertIs<RestoreCommand>(page.elements[5])
    }

    @Test
    fun preservesInterpolationTokensVerbatim() {
        val expect = page.elements[4] as ExpectFileCommand
        assertEquals("${'$'}{{ project_root }}/out.txt", expect.path)
    }

    @Test
    fun mixesCommandsAndPageInvocationsWithoutASequentialDirective() {
        val backUp = page.elements[0] as BackUpCommand
        assertEquals("backup.tar.gz", backUp.fileName)
        val invocation = page.elements[1] as PageInvocation
        assertEquals("Do a thing", invocation.name)
        val restore = page.elements[5] as RestoreCommand
        assertEquals(null, restore.backupName)
    }

    @Test
    fun parsesNamedRestoreForm() {
        val page = parsePage("# P\n* flik version: `0.1`\n\n* restore from backup `older.tar.gz`")
        val restore = assertIs<RestoreCommand>(page.elements.single())
        assertEquals("older.tar.gz", restore.backupName)
    }

    @Test
    fun parsesInlineRunCommandAndExpectToSee() {
        val page = parsePage("# P\n* flik version: `0.1`\n\n* run command: `echo hi`\n* expect to see `hi`")
        assertEquals(2, page.elements.size)
        assertEquals("echo hi", (page.elements[0] as RunShellCommand).command)
        val expect = assertIs<ExpectToSeeCommand>(page.elements[1])
        assertEquals("hi", expect.expected)
    }

    @Test
    fun requiresTitle() {
        assertFailsWith<FlikParseException> { parsePage("no heading here") }
    }
}
