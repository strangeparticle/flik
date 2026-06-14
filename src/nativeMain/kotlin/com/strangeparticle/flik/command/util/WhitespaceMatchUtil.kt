package com.strangeparticle.flik.command.util

/**
 * Whitespace-insensitive text matching for poll output comparison. All whitespace is
 * *removed* (not collapsed to single spaces) before comparing, so differences in
 * JSON/text formatting — indentation, line breaks, spacing around punctuation — never
 * affect the result. Removing whitespace entirely is what makes JSON formatting genuinely
 * irrelevant (e.g. `{ "ok": true }` matches `{"ok":true}`).
 */

/** [text] with every whitespace character removed. */
fun normalizeWhitespace(text: String): String = text.filterNot { it.isWhitespace() }

/** True once [needle] appears in [haystack] with all whitespace removed from both. */
fun containsIgnoringWhitespace(haystack: String, needle: String): Boolean =
    normalizeWhitespace(haystack).contains(normalizeWhitespace(needle))
