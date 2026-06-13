package com.strangeparticle.flik.program

import com.strangeparticle.flik.command.parsePage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun resultOf(vararg pages: Pair<String, String>): CompileResult {
    val parsed = pages.associate { (path, source) -> path to parsePage(source) }
    return CompileResult(pages.first().first, parsed, emptyMap(), emptyList())
}

class PreflightTest {
    @Test
    fun aggregatesEveryMissingPrerequisiteAcrossAllPages() {
        val result = resultOf(
            "/p/root.flik.md" to "# Root\n\n* environment variable: `APPLE_ID`\n* shell command: `xcrun`",
            "/p/child.flik.md" to "# Child\n\n* environment variable: `APPLE_TEAM_ID`",
        )
        val problems = preflight(
            result,
            environmentVariable = { null },   // nothing set
            commandAvailable = { false },     // nothing available
        )
        assertEquals(3, problems.size)
        assertTrue(problems.any { it.contains("APPLE_ID") })
        assertTrue(problems.any { it.contains("APPLE_TEAM_ID") })
        assertTrue(problems.any { it.contains("xcrun") })
    }

    @Test
    fun reportsNothingWhenAllSatisfied() {
        val result = resultOf("/p/root.flik.md" to "# Root\n\n* environment variable: `X`\n* shell command: `git`")
        val problems = preflight(result, environmentVariable = { "set" }, commandAvailable = { true })
        assertEquals(emptyList(), problems)
    }

    @Test
    fun reportsUnsetEnvironmentVariableReferencedViaInterpolation() {
        // The page never declares STAGE_DIR as a prerequisite — it only references it via
        // `${'$'}{{ env.STAGE_DIR }}` in an `Expect file to exist:` path. Preflight must
        // still flag it as unset, alongside the missing-prerequisite list.
        val result = resultOf(
            "/p/root.flik.md" to "# Root\n\nExpect file to exist: `${'$'}{{ env.STAGE_DIR }}/out.txt`\n",
        )
        val problems = preflight(result, environmentVariable = { null }, commandAvailable = { true })
        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("STAGE_DIR"))
        assertTrue(problems.single().contains("root.flik.md"))
    }

    @Test
    fun referencedEnvironmentVariableThatIsSetIsNotReported() {
        val result = resultOf(
            "/p/root.flik.md" to "# Root\n\nExpect file to exist: `${'$'}{{ env.STAGE_DIR }}/out.txt`\n",
        )
        val problems = preflight(result, environmentVariable = { "set" }, commandAvailable = { true })
        assertEquals(emptyList(), problems)
    }

    @Test
    fun declaredAndReferencedEnvironmentVariableIsReportedOnceWithBothPages() {
        // FOO is both a declared prerequisite on root and referenced via interpolation on
        // child. It should appear once, attributed to both pages.
        val result = resultOf(
            "/p/root.flik.md" to "# Root\n\n* environment variable: `FOO`",
            "/p/child.flik.md" to "# Child\n\nExpect file to exist: `${'$'}{{ env.FOO }}/x`\n",
        )
        val problems = preflight(result, environmentVariable = { null }, commandAvailable = { true })
        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("FOO"))
        assertTrue(problems.single().contains("root.flik.md"))
        assertTrue(problems.single().contains("child.flik.md"))
    }
}
