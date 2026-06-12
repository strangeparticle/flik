package validate

import command.BackUpStep
import command.CalloutStep
import command.ProcedureStep
import command.RestoreStep
import model.EntryDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun entryDocument(
    procedure: List<ProcedureStep>,
    flikVersion: String? = "0.1",
) = EntryDocument(
    title = "Test",
    flikVersion = flikVersion,
    requiredEnvironmentVariables = emptyList(),
    requiredShellCommands = emptyList(),
    procedure = procedure,
)

class EntryDocumentValidatorTest {
    @Test
    fun flagsMissingCalloutFile() {
        val problems = findEntryDocumentProblems(
            entryDocument(listOf(CalloutStep("Build the signed, notarized DMG"))),
            calloutFileExists = { false },
        )
        assertEquals(1, problems.size)
        assertTrue(problems[0].contains("build-the-signed-notarized-dmg.flik.md"))
    }

    @Test
    fun passesWhenCalloutsResolve() {
        val problems = findEntryDocumentProblems(
            entryDocument(
                listOf(
                    BackUpStep("backup.tar.gz"),
                    CalloutStep("Anything"),
                    RestoreStep(),
                ),
            ),
            calloutFileExists = { true },
        )
        assertEquals(emptyList(), problems)
    }

    @Test
    fun flagsMissingFlikVersion() {
        val problems = findEntryDocumentProblems(
            entryDocument(emptyList(), flikVersion = null),
            calloutFileExists = { true },
        )
        assertTrue(problems.any { it.contains("flik version") })
    }
}
