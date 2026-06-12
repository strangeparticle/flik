package com.strangeparticle.flik.program

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FOOTER = "\n\n---\nAuthored for compatibility with Flik `v0.1`"

/** Builds an in-memory filesystem (read + exists) from path -> source pairs. */
private fun fakeFs(vararg files: Pair<String, String>): Pair<(String) -> String, (String) -> Boolean> {
    val map = files.toMap()
    val read = { path: String -> map[path] ?: throw RuntimeException("no such file: $path") }
    val exists = { path: String -> map.containsKey(path) }
    return read to exists
}

class LinkerTest {
    @Test
    fun linksAcyclicGraphParsingEachPageOnce() {
        val (read, exists) = fakeFs(
            "/p/root.flik.md" to "# Root\n\n* [Child A]\n* [Child B]$FOOTER",
            "/p/child-a.flik.md" to "# Child A\n\n* [Shared]$FOOTER",
            "/p/child-b.flik.md" to "# Child B\n\n* [Shared]$FOOTER",
            "/p/shared.flik.md" to "# Shared$FOOTER",
        )
        val result = compile("/p/root.flik.md", read, exists)
        assertEquals(emptyList(), result.diagnostics)
        assertEquals(
            setOf("/p/root.flik.md", "/p/child-a.flik.md", "/p/child-b.flik.md", "/p/shared.flik.md"),
            result.pages.keys,
        )
    }

    @Test
    fun reportsMissingInvokedPage() {
        val (read, exists) = fakeFs("/p/root.flik.md" to "# Root\n\n* [Nope]$FOOTER")
        val result = compile("/p/root.flik.md", read, exists)
        assertTrue(result.diagnostics.any { it.message.contains("nope.flik.md not found") })
    }

    @Test
    fun detectsCycle() {
        val (read, exists) = fakeFs(
            "/p/a.flik.md" to "# A\n\n* [B]$FOOTER",
            "/p/b.flik.md" to "# B\n\n* [A]$FOOTER",
        )
        val result = compile("/p/a.flik.md", read, exists)
        assertTrue(result.diagnostics.any { it.message.contains("cycle") })
    }

    @Test
    fun reportsParseErrorWithPageProvenance() {
        val (read, exists) = fakeFs("/p/root.flik.md" to "no title here")
        val result = compile("/p/root.flik.md", read, exists)
        assertEquals("/p/root.flik.md", result.diagnostics.first().page)
        assertTrue(result.diagnostics.first().message.contains("parse error"))
    }

    @Test
    fun flagsMissingVersionFooter() {
        val (read, exists) = fakeFs("/p/root.flik.md" to "# Root\n")
        val result = compile("/p/root.flik.md", read, exists)
        assertTrue(result.diagnostics.any { it.message.contains("version footer") })
    }
}
