package parse

import model.ProcedureStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val SAMPLE = """
    # Release Springboard for macOS (Direct Download DMG)

    # Pre-requisites
    flik version: 0.1

    Environment Variables:
    * APPLE_ID
    * APPLE_APP_SPECIFIC_PASSWORD
    * APPLE_TEAM_ID

    Shell Commands:
    * ./gradlew
    * xcrun

    # Release Procedure
    Run these steps sequentially, in the order shown:
    * back up to file `../springboard-release-backup.tar.gz`
    * [Re-apply release-only repo changes]
    * [Build the signed, notarized DMG]
    * restore the repository
""".trimIndent()

class EntryDocumentParserTest {
    @Test
    fun parsesTitleAndVersion() {
        val doc = parseEntryDocument(SAMPLE)
        assertEquals("Release Springboard for macOS (Direct Download DMG)", doc.title)
        assertEquals("0.1", doc.flikVersion)
    }

    @Test
    fun parsesPrerequisiteLists() {
        val doc = parseEntryDocument(SAMPLE)
        assertEquals(
            listOf("APPLE_ID", "APPLE_APP_SPECIFIC_PASSWORD", "APPLE_TEAM_ID"),
            doc.requiredEnvironmentVariables,
        )
        assertEquals(listOf("./gradlew", "xcrun"), doc.requiredShellCommands)
    }

    @Test
    fun parsesProcedureStepsInOrder() {
        val doc = parseEntryDocument(SAMPLE)
        assertEquals(
            listOf(
                ProcedureStep.BackUpToFile("../springboard-release-backup.tar.gz"),
                ProcedureStep.Callout("Re-apply release-only repo changes"),
                ProcedureStep.Callout("Build the signed, notarized DMG"),
                ProcedureStep.RestoreRepository,
            ),
            doc.procedure,
        )
    }

    @Test
    fun rejectsUnknownStep() {
        val bad = SAMPLE.replace("* restore the repository", "* teleport the repository")
        assertFailsWith<FlikParseException> { parseEntryDocument(bad) }
    }

    @Test
    fun requiresTitle() {
        assertFailsWith<FlikParseException> { parseEntryDocument("no heading here") }
    }
}
