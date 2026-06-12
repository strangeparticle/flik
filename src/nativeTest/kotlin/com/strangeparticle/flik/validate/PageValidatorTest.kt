package com.strangeparticle.flik.validate

import com.strangeparticle.flik.command.BackUpCommand
import com.strangeparticle.flik.command.Page
import com.strangeparticle.flik.command.PageElement
import com.strangeparticle.flik.command.PageInvocation
import com.strangeparticle.flik.command.RestoreCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun page(
    elements: List<PageElement>,
    flikVersion: String? = "0.1",
) = Page(
    title = "Test",
    flikVersion = flikVersion,
    requiredEnvironmentVariables = emptyList(),
    requiredShellCommands = emptyList(),
    elements = elements,
)

class PageValidatorTest {
    @Test
    fun flagsMissingInvokedPage() {
        val problems = findPageProblems(
            page(listOf(PageInvocation("Build the signed, notarized DMG"))),
            pageFileExists = { false },
        )
        assertEquals(1, problems.size)
        assertTrue(problems[0].contains("build-the-signed-notarized-dmg.flik.md"))
    }

    @Test
    fun passesWhenInvocationsResolve() {
        val problems = findPageProblems(
            page(listOf(BackUpCommand("b.tar.gz"), PageInvocation("Anything"), RestoreCommand(null))),
            pageFileExists = { true },
        )
        assertEquals(emptyList(), problems)
    }

    @Test
    fun flagsMissingFlikVersion() {
        val problems = findPageProblems(
            page(emptyList(), flikVersion = null),
            pageFileExists = { true },
        )
        assertTrue(problems.any { it.contains("flik version") })
    }
}
