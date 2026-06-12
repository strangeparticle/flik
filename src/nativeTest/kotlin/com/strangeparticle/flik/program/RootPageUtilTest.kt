package com.strangeparticle.flik.program

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RootPageUtilTest {
    @Test
    fun resolvesADirectoryToItsFolderNamedRoot() {
        val resolution = resolveRootPage(
            "examples/release-macos-dmg",
            isDirectory = { it == "examples/release-macos-dmg" },
            fileExists = { it == "examples/release-macos-dmg/release-macos-dmg.flik.md" },
        )
        assertEquals(
            RootPageResolution.Found("examples/release-macos-dmg/release-macos-dmg.flik.md"),
            resolution,
        )
    }

    @Test
    fun passesAFilePathThrough() {
        val resolution = resolveRootPage(
            "examples/release-macos-dmg/release-macos-dmg.flik.md",
            isDirectory = { false },
            fileExists = { true },
        )
        assertEquals(
            RootPageResolution.Found("examples/release-macos-dmg/release-macos-dmg.flik.md"),
            resolution,
        )
    }

    @Test
    fun reportsWhenADirectoryHasNoMatchingRoot() {
        val resolution = resolveRootPage(
            "examples/empty",
            isDirectory = { true },
            fileExists = { false },
        )
        val notFound = assertIs<RootPageResolution.NotFound>(resolution)
        assertEquals("no root page in examples/empty: expected empty.flik.md", notFound.message)
    }

    @Test
    fun toleratesATrailingSlash() {
        val resolution = resolveRootPage(
            "examples/release-macos-dmg/",
            isDirectory = { true },
            fileExists = { it == "examples/release-macos-dmg/release-macos-dmg.flik.md" },
        )
        assertEquals(
            RootPageResolution.Found("examples/release-macos-dmg/release-macos-dmg.flik.md"),
            resolution,
        )
    }
}
