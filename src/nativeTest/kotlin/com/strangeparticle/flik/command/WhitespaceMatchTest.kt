package com.strangeparticle.flik.command.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhitespaceMatchTest {
    @Test
    fun removesSpacesTabsAndNewlines() {
        assertEquals("abcd", normalizeWhitespace("a b\tc\nd"))
    }

    @Test
    fun removesLeadingTrailingAndCarriageReturnWhitespace() {
        assertEquals("xy", normalizeWhitespace(" \tx\r\ny\n "))
    }

    @Test
    fun matchesAWhitespaceFormattedSubPortion() {
        // Formatting differs; the example is a sub-portion of the larger response.
        assertTrue(
            containsIgnoringWhitespace("""{"state":"success","sha":"abc"}""", """"state": "success""""),
        )
    }

    @Test
    fun matchesAFullObjectExampleWhenTheWholeObjectIsPresent() {
        val response = "prefix\n{\n  \"ok\": true\n}\nsuffix"
        assertTrue(containsIgnoringWhitespace(response, "{ \"ok\": true }"))
    }

    @Test
    fun doesNotMatchAFullObjectExampleWhenExtraFieldsBreakContiguity() {
        // Substring (not structural) matching: the closing brace is not adjacent to "success".
        assertFalse(
            containsIgnoringWhitespace("""{"state":"success","sha":"abc"}""", """{ "state": "success" }"""),
        )
    }

    @Test
    fun doesNotMatchWhenContentDiffers() {
        assertFalse(containsIgnoringWhitespace("""{"state":"failed"}""", """"state": "success""""))
    }

    @Test
    fun anEmptyExampleAlwaysMatches() {
        assertTrue(containsIgnoringWhitespace("anything at all", "   "))
    }
}
