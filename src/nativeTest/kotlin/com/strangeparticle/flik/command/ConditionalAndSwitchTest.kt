package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.commands.CommandAvailable
import com.strangeparticle.flik.command.commands.ConditionalCommand
import com.strangeparticle.flik.command.commands.FileExists
import com.strangeparticle.flik.command.commands.RunCommand
import com.strangeparticle.flik.command.commands.SwitchCommand
import com.strangeparticle.flik.command.commands.ValueEquals
import com.strangeparticle.flik.parse.FlikParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class ConditionalAndSwitchTest {
    @Test
    fun parsesIfOtherwiseWithACommandAvailablePredicate() {
        val page = parsePage(
            "# P\n\n" +
                "If the command `xcodebuild` is available:\n\n" +
                "* run command: `echo with-xcode`\n\n" +
                "Otherwise:\n\n" +
                "* run command: `echo without-xcode`",
        )
        val conditional = assertIs<ConditionalCommand>(page.elements.single())
        val predicate = assertIs<CommandAvailable>(conditional.predicate)
        assertEquals("xcodebuild", predicate.command)
        assertEquals("echo with-xcode", assertIs<RunCommand>(conditional.thenBranch).command)
        assertEquals("echo without-xcode", assertIs<RunCommand>(conditional.elseBranch).command)
    }

    @Test
    fun parsesIfWithNoOtherwise() {
        val page = parsePage("# P\n\nIf the file `VERSION` exists:\n\n* run command: `cat VERSION`")
        val conditional = assertIs<ConditionalCommand>(page.elements.single())
        assertIs<FileExists>(conditional.predicate)
        assertNull(conditional.elseBranch)
    }

    @Test
    fun branchCanBeAPageInvocationAndIsReportedForLinking() {
        val page = parsePage("# P\n\nIf the command `git` is available:\n\n* [Commit the change]")
        val conditional = assertIs<ConditionalCommand>(page.elements.single())
        assertIs<PageInvocation>(conditional.thenBranch)
        // The linker must see invocations nested inside the branch, not just top-level ones.
        assertEquals(listOf("Commit the change"), conditional.referencedPageInvocations().map { it.name })
    }

    @Test
    fun leavesIfTheCommandFailsForTheRunCommandModifier() {
        // `If the command fails:` is the inline run-command modifier, not a block conditional.
        val page = parsePage("# P\n\nRun command:\n\n```shell\nprimary\n```\n\nIf the command fails:\n\n```shell\nfallback\n```")
        val run = assertIs<RunCommand>(page.elements.single())
        assertEquals("fallback", run.fallbackCommand)
    }

    @Test
    fun parsesSwitchWithLiteralBranchesAndOtherwise() {
        val page = parsePage(
            "# P\n\n" +
                "Depending on `${'$'}{{ capture.stage }}`:\n\n" +
                "* `prod`: [Deploy to production]\n" +
                "* `dev`: run command: `echo dev`\n" +
                "* otherwise: [Choose a stage]",
        )
        val switch = assertIs<SwitchCommand>(page.elements.single())
        assertEquals("${'$'}{{ capture.stage }}", switch.selector)
        assertEquals(listOf("prod", "dev"), switch.branches.map { it.literal })
        assertIs<PageInvocation>(switch.branches[0].element)
        assertEquals("echo dev", assertIs<RunCommand>(switch.branches[1].element).command)
        assertIs<PageInvocation>(switch.otherwise)
        // prod page + otherwise page are both reported for linking; the inline command is not a page.
        assertEquals(
            listOf("Deploy to production", "Choose a stage"),
            switch.referencedPageInvocations().map { it.name },
        )
    }

    @Test
    fun switchValueEqualsPredicateRoundTrips() {
        val page = parsePage("# P\n\nIf `${'$'}{{ capture.stage }}` is `prod`:\n\n* run command: `echo go`")
        val conditional = assertIs<ConditionalCommand>(page.elements.single())
        val predicate = assertIs<ValueEquals>(conditional.predicate)
        assertEquals("${'$'}{{ capture.stage }}", predicate.value)
        assertEquals("prod", predicate.literal)
    }

    @Test
    fun failsWhenABranchHasNoStep() {
        assertFailsWith<FlikParseException> { parsePage("# P\n\nIf the command `git` is available:") }
    }
}
