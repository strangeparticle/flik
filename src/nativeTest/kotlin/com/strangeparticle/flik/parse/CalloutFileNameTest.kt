package com.strangeparticle.flik.parse

import kotlin.test.Test
import kotlin.test.assertEquals

class CalloutFileNameTest {
    @Test
    fun normalizesHyphenatedName() {
        assertEquals(
            "re-apply-release-only-repo-changes.flik.md",
            calloutFileName("Re-apply release-only repo changes"),
        )
    }

    @Test
    fun stripsCommaAndCollapsesWhitespace() {
        assertEquals(
            "build-the-signed-notarized-dmg.flik.md",
            calloutFileName("Build the signed, notarized DMG"),
        )
    }

    @Test
    fun keepsInteriorWords() {
        assertEquals(
            "verify-the-dmg-signature-and-notarization.flik.md",
            calloutFileName("Verify the DMG signature and notarization"),
        )
    }

    @Test
    fun trimsSurroundingPunctuationAndWhitespace() {
        assertEquals(
            "check-release-prerequisites.flik.md",
            calloutFileName("  Check release prerequisites!  "),
        )
    }
}
