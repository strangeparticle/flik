package com.strangeparticle.flik.os

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.usleep

/**
 * Sleeps for [millis] milliseconds via posix `usleep`, in sub-second chunks (`usleep` is
 * specified only for waits under one second). A thin OS wrapper, like the rest of `os/`.
 */
@OptIn(ExperimentalForeignApi::class)
fun sleepMillis(millis: Long) {
    if (millis <= 0) return
    var remainingMicros = millis * 1_000
    val chunkMicros = 500_000L
    while (remainingMicros > 0) {
        val thisChunk = minOf(chunkMicros, remainingMicros)
        usleep(thisChunk.toUInt())
        remainingMicros -= thisChunk
    }
}
