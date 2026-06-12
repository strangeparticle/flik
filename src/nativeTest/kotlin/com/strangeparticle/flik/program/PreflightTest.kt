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
}
