package com.strangeparticle.flik.command.util

/** The first line of [text], trimmed and length-capped, for compact log output. */
fun firstLine(text: String): String {
    val line = text.lineSequence().firstOrNull()?.trim().orEmpty()
    return if (line.length > 100) line.take(99) + "…" else line
}
