package command

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

class StageDocumentParserTest {
    private val steps = parseStageDocument(SAMPLE).steps

    @Test
    fun parsesAllStepsInOrder() {
        assertEquals(5, steps.size)
        assertIs<CopyFileStep>(steps[0])
        assertIs<EditInPlaceStep>(steps[1])
        assertIs<RunCommandStep>(steps[2])
        assertIs<ExpectFileExistsStep>(steps[3])
        assertIs<RunChecksStep>(steps[4])
    }

    @Test
    fun parsesCopyAndEditFields() {
        val copy = steps[0] as CopyFileStep
        assertEquals("./reference/a.txt", copy.from)
        assertEquals("dest/a.txt", copy.to)

        val edit = steps[1] as EditInPlaceStep
        assertEquals("config.kts", edit.file)
        assertEquals("old()", edit.find)
        assertEquals("new()", edit.replace)
    }

    @Test
    fun parsesRunAndExpect() {
        assertEquals("echo hi", (steps[2] as RunCommandStep).command)
        assertEquals("{{ project_root }}/out.txt", (steps[3] as ExpectFileExistsStep).path)
    }

    @Test
    fun parsesParallelChecksWithOptionalContains() {
        val checks = (steps[4] as RunChecksStep).checks
        assertEquals(2, checks.size)
        assertEquals("echo alpha", checks[0].command)
        assertEquals("alpha", checks[0].expectContains)
        assertEquals("echo beta", checks[1].command)
        assertNull(checks[1].expectContains)
    }
}
