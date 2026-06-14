package com.strangeparticle.flik.command.util

import com.strangeparticle.flik.parse.FlikParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DurationParseTest {
    @Test fun parsesSeconds() = assertEquals(30_000L, parseDurationMillis("30s"))

    @Test fun parsesMinutes() = assertEquals(600_000L, parseDurationMillis("10m"))

    @Test fun parsesHours() = assertEquals(3_600_000L, parseDurationMillis("1h"))

    @Test fun acceptsAnUppercaseUnit() = assertEquals(5_000L, parseDurationMillis("5S"))

    @Test fun toleratesSurroundingWhitespace() = assertEquals(5_000L, parseDurationMillis(" 5s "))

    @Test fun rejectsAMissingUnit() {
        assertFailsWith<FlikParseException> { parseDurationMillis("5") }
    }

    @Test fun rejectsAnUnknownUnit() {
        assertFailsWith<FlikParseException> { parseDurationMillis("5x") }
    }

    @Test fun rejectsANonNumber() {
        assertFailsWith<FlikParseException> { parseDurationMillis("abc") }
    }

    @Test fun rejectsEmptyText() {
        assertFailsWith<FlikParseException> { parseDurationMillis("") }
    }

    @Test fun rejectsZero() {
        assertFailsWith<FlikParseException> { parseDurationMillis("0s") }
    }

    @Test fun rejectsACompositeDuration() {
        assertFailsWith<FlikParseException> { parseDurationMillis("1m30s") }
    }
}
