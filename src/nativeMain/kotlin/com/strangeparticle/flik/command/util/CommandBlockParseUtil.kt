package com.strangeparticle.flik.command.util

/**
 * Parses a labeled fenced-command block: a line equal to [label] (e.g. `Run command:`,
 * `Poll by running:`) followed by a fenced code block. Returns the trimmed block body and
 * the index just past it, or null if [label] is not on `lines[index]`. Shared by the run
 * and poll commands so their primary-command block is read identically.
 */
fun parseLabeledCommandBlock(lines: List<String>, index: Int, label: String): Pair<String, Int>? {
    if (!lines[index].trim().equals(label, ignoreCase = true)) return null
    val (block, next) = readFencedBlock(lines, index + 1)
    return block.trim() to next
}
