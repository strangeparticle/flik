package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.interpolate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InterpolationTest {
    @Test
    fun substitutesCaptureValues() {
        assertEquals(
            "build-2026-abc",
            interpolate(
                "build-${'$'}{{ capture.year }}-${'$'}{{ capture.sha }}",
                "/proj",
                mapOf("year" to "2026", "sha" to "abc"),
            ),
        )
    }

    @Test
    fun failsOnUnknownCapture() {
        assertFailsWith<FlikExecutionException> { interpolate("${'$'}{{ capture.missing }}", "/proj") }
    }

    @Test
    fun substitutesProjectRootWithSpaces() {
        assertEquals(
            "/proj/desktopApp/x.dmg",
            interpolate("${'$'}{{ project_root }}/desktopApp/x.dmg", "/proj"),
        )
    }

    @Test
    fun substitutesWithoutInnerSpaces() {
        assertEquals("/proj/a", interpolate("${'$'}{{project_root}}/a", "/proj"))
    }

    @Test
    fun leavesShellStyleDollarBracesUntouched() {
        // A plain ${...} (no double brace) is NOT Flik interpolation — it passes through.
        assertEquals("\${HOME}/a", interpolate("\${HOME}/a", "/proj"))
    }

    @Test
    fun leavesOtherTextUntouched() {
        assertEquals("no tokens here", interpolate("no tokens here", "/proj"))
    }
}
