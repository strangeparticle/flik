package parse

import model.StageCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private val SAMPLE = """
    # Sample Stage

    flik version: 0.1

    ## Copy stuff

    * Copy file from: `./reference/a.txt` to: `dest/a.txt`

    ## Edit the config

    In file: `config.kts`

    Find this section:

    ```kotlin
    old()
    ```

    Replace it with:

    ```kotlin
    new()
    ```

    ## Build

    Run command:

    ```shell
    echo hi
    ```

    Expect file to exist: `{{ project_root }}/out.txt`

    ## Checks

    Run these checks in parallel:

    * **One** — run `echo alpha` and expect the output to contain `alpha`.
    * **Two** — run `echo beta` and report stuff.
""".trimIndent()

class SubDocumentParserTest {
    private val commands = parseSubDocument(SAMPLE).commands

    @Test
    fun parsesAllCommandsInOrder() {
        assertEquals(5, commands.size)
        assertIs<StageCommand.CopyFile>(commands[0])
        assertIs<StageCommand.EditInPlace>(commands[1])
        assertIs<StageCommand.RunCommand>(commands[2])
        assertIs<StageCommand.ExpectFileExists>(commands[3])
        assertIs<StageCommand.ParallelChecks>(commands[4])
    }

    @Test
    fun parsesCopyAndEditFields() {
        val copy = commands[0] as StageCommand.CopyFile
        assertEquals("./reference/a.txt", copy.from)
        assertEquals("dest/a.txt", copy.to)

        val edit = commands[1] as StageCommand.EditInPlace
        assertEquals("config.kts", edit.file)
        assertEquals("old()", edit.find)
        assertEquals("new()", edit.replace)
    }

    @Test
    fun parsesRunAndExpect() {
        assertEquals("echo hi", (commands[2] as StageCommand.RunCommand).command)
        assertEquals("{{ project_root }}/out.txt", (commands[3] as StageCommand.ExpectFileExists).path)
    }

    @Test
    fun parsesParallelChecksWithOptionalContains() {
        val checks = (commands[4] as StageCommand.ParallelChecks).checks
        assertEquals(2, checks.size)
        assertEquals("echo alpha", checks[0].command)
        assertEquals("alpha", checks[0].expectContains)
        assertEquals("echo beta", checks[1].command)
        assertNull(checks[1].expectContains)
    }
}
