package com.strangeparticle.flik.command.util

private val CAPTURE_REGEX = Regex("capture output as:?\\s*`(.+)`", RegexOption.IGNORE_CASE)

/**
 * The capture name if [content] is a `capture output as: \`NAME\`` modifier line, else null.
 * Shared by the run and poll commands so the capture syntax stays identical in both.
 */
fun parseCaptureModifier(content: String): String? =
    CAPTURE_REGEX.matchEntire(content)?.groupValues?.get(1)
