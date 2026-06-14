package com.strangeparticle.flik

import kotlin.test.Test
import kotlin.test.assertTrue

class FlikVersionTest {
    // Matches SemVer: MAJOR.MINOR.PATCH with optional -prerelease and +build metadata.
    private val semverPattern = Regex("""^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?(\+[0-9A-Za-z.-]+)?$""")

    @Test
    fun versionIsNotBlank() {
        assertTrue(FlikVersion.VERSION.isNotBlank())
    }

    @Test
    fun versionIsValidSemver() {
        assertTrue(
            semverPattern.matches(FlikVersion.VERSION),
            "FlikVersion.VERSION is not a valid SemVer string: '${FlikVersion.VERSION}'",
        )
    }
}
