package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.commands.PollForStatusCommand
import com.strangeparticle.flik.parse.FlikParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

private val FULL = """
    # P

    Poll by running:

    ```shell
    curl -sf https://ci.example.com/status
    ```

    Until the output contains:

    ```json
    { "state": "success" }
    ```

    * time out after: `10m`
    * check every: `15s`
    * capture output as: `build_status`
""".trimIndent()

class PollForStatusTest {
    @Test
    fun parsesAllFields() {
        val poll = assertIs<PollForStatusCommand>(parsePage(FULL).elements.single())
        assertEquals("curl -sf https://ci.example.com/status", poll.command)
        assertEquals("{ \"state\": \"success\" }", poll.expectedOutput)
        assertEquals(600_000L, poll.timeoutMillis)
        assertEquals(15_000L, poll.intervalMillis)
        assertEquals("build_status", poll.captureAs)
    }

    @Test
    fun intervalDefaultsToFiveSecondsAndCaptureIsOptional() {
        val doc = """
            # P

            Poll by running:

            ```shell
            check.sh
            ```

            Until the output contains:

            ```text
            ready
            ```

            * time out after: `2m`
        """.trimIndent()
        val poll = assertIs<PollForStatusCommand>(parsePage(doc).elements.single())
        assertEquals(5_000L, poll.intervalMillis)
        assertNull(poll.captureAs)
    }

    @Test
    fun aMissingTimeoutIsAParseError() {
        val doc = """
            # P

            Poll by running:

            ```shell
            check.sh
            ```

            Until the output contains:

            ```text
            ready
            ```

            * check every: `5s`
        """.trimIndent()
        assertFailsWith<FlikParseException> { parsePage(doc) }
    }

    @Test
    fun aMalformedDurationIsAParseError() {
        val doc = """
            # P

            Poll by running:

            ```shell
            check.sh
            ```

            Until the output contains:

            ```text
            ready
            ```

            * time out after: `soon`
        """.trimIndent()
        assertFailsWith<FlikParseException> { parsePage(doc) }
    }

    @Test
    fun aMissingUntilBlockIsAParseError() {
        val doc = """
            # P

            Poll by running:

            ```shell
            check.sh
            ```

            * time out after: `2m`
        """.trimIndent()
        assertFailsWith<FlikParseException> { parsePage(doc) }
    }

    @Test
    fun preservesInterpolationTokensInCommandAndExample() {
        val doc = """
            # P

            Poll by running:

            ```shell
            curl -sf ${'$'}{{ env.STATUS_URL }}
            ```

            Until the output contains:

            ```json
            { "sha": "${'$'}{{ capture.sha }}" }
            ```

            * time out after: `1m`
        """.trimIndent()
        val poll = assertIs<PollForStatusCommand>(parsePage(doc).elements.single())
        assertEquals("curl -sf ${'$'}{{ env.STATUS_URL }}", poll.command)
        assertEquals("{ \"sha\": \"${'$'}{{ capture.sha }}\" }", poll.expectedOutput)
    }

    @Test
    fun executeMatchesImmediatelyAndCapturesTheOutput() {
        val context = ExecutionContext(
            projectRoot = "/",
            backupsDirectory = "/tmp",
            pages = emptyMap(),
            log = {},
        )
        val poll = PollForStatusCommand(
            command = "echo '{\"state\":\"success\"}'",
            expectedOutput = "{ \"state\": \"success\" }",
            timeoutMillis = 5_000,
            intervalMillis = 1_000,
            captureAs = "result",
        )
        poll.execute(context)
        assertEquals("{\"state\":\"success\"}", context.interpolate("${'$'}{{ capture.result }}"))
    }
}
