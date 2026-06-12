package parse

import command.BackUpStep
import command.CalloutStep
import command.RestoreStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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
        val steps = parseEntryDocument(SAMPLE).procedure
        assertEquals(4, steps.size)

        val backUp = assertIs<BackUpStep>(steps[0])
        assertEquals("../springboard-release-backup.tar.gz", backUp.path)

        val firstCallout = assertIs<CalloutStep>(steps[1])
        assertEquals("Re-apply release-only repo changes", firstCallout.name)

        assertIs<CalloutStep>(steps[2])
        assertIs<RestoreStep>(steps[3])
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
