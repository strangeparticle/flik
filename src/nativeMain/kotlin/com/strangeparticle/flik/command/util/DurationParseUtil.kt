package com.strangeparticle.flik.command.util

import com.strangeparticle.flik.parse.FlikParseException

private val DURATION_REGEX = Regex("(\\d+)\\s*([smh])", RegexOption.IGNORE_CASE)

/**
 * Parses an author-written duration like `30s`, `10m`, or `1h` into milliseconds. A
 * duration is a positive integer followed by a single unit: `s` (seconds), `m` (minutes),
 * or `h` (hours). Anything else — no unit, an unknown unit, a non-number, a composite like
 * `1m30s`, or a non-positive value — is a parse error, surfaced by `flik validate` before
 * any execution.
 */
fun parseDurationMillis(text: String): Long {
    val match = DURATION_REGEX.matchEntire(text.trim())
        ?: throw FlikParseException(
            "invalid duration \"$text\" — use a positive integer with a unit s, m, or h (e.g. 30s, 10m, 1h)",
        )
    val value = match.groupValues[1].toLong()
    if (value <= 0) {
        throw FlikParseException("duration must be positive: \"$text\"")
    }
    val unitMillis = when (match.groupValues[2].lowercase()) {
        "s" -> 1_000L
        "m" -> 60_000L
        "h" -> 3_600_000L
        else -> throw FlikParseException("unknown duration unit in \"$text\"")
    }
    return value * unitMillis
}
