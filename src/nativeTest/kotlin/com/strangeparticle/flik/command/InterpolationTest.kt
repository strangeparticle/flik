package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.environmentReferences
import com.strangeparticle.flik.command.util.interpolate
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.setenv
import platform.posix.unsetenv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalForeignApi::class)
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

    @Test
    fun substitutesSetEnvironmentVariable() {
        setenv("FLIK_TEST_ENV_NAME", "/home/tester", 1)
        try {
            assertEquals(
                "/home/tester/profile.txt",
                interpolate("${'$'}{{ env.FLIK_TEST_ENV_NAME }}/profile.txt", "/proj"),
            )
        } finally {
            unsetenv("FLIK_TEST_ENV_NAME")
        }
    }

    @Test
    fun substitutesEnvironmentVariableWithoutInnerSpaces() {
        setenv("FLIK_TEST_ENV_TIGHT", "value", 1)
        try {
            assertEquals("value-x", interpolate("${'$'}{{env.FLIK_TEST_ENV_TIGHT}}-x", "/proj"))
        } finally {
            unsetenv("FLIK_TEST_ENV_TIGHT")
        }
    }

    @Test
    fun unsetEnvironmentVariableFailsTheRun() {
        // Hard backstop: an unset env var is not silently substituted to "" — it fails.
        // (In normal use preflight reports it before execution ever reaches interpolation.)
        unsetenv("FLIK_TEST_ENV_MISSING")
        assertFailsWith<FlikExecutionException> {
            interpolate("${'$'}{{ env.FLIK_TEST_ENV_MISSING }}/a", "/proj")
        }
    }

    @Test
    fun projectRootCaptureAndEnvInterpolateTogether() {
        setenv("FLIK_TEST_ENV_BOTH", "leaf", 1)
        try {
            assertEquals(
                "/proj/abc/leaf",
                interpolate(
                    "${'$'}{{ project_root }}/${'$'}{{ capture.sha }}/${'$'}{{ env.FLIK_TEST_ENV_BOTH }}",
                    "/proj",
                    mapOf("sha" to "abc"),
                ),
            )
        } finally {
            unsetenv("FLIK_TEST_ENV_BOTH")
        }
    }

    @Test
    fun environmentReferencesAreExtractedDistinctAndInOrder() {
        val references = environmentReferences(
            "${'$'}{{ env.HOME }}/${'$'}{{ env.STAGE }} then ${'$'}{{ env.HOME }} again",
        )
        assertEquals(listOf("HOME", "STAGE"), references)
    }

    @Test
    fun environmentReferencesIgnoresProjectRootAndPlainShellVars() {
        val references = environmentReferences("${'$'}{{ project_root }}/\${HOME}/\$PATH")
        assertEquals(emptyList(), references)
    }
}
