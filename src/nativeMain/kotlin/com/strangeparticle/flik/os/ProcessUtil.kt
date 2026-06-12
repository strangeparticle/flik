package com.strangeparticle.flik.os

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.getenv
import platform.posix.pclose
import platform.posix.popen

/** The combined (stdout+stderr) output and exit code of a shell command. */
data class ShellResult(val exitCode: Int, val output: String)

/**
 * Runs [command] through `/bin/sh`, capturing combined stdout+stderr, optionally
 * from [workingDirectory]. The exit code is decoded from the wait status.
 */
@OptIn(ExperimentalForeignApi::class)
fun runShellCommand(command: String, workingDirectory: String? = null): ShellResult {
    val prefixed = if (workingDirectory != null) {
        "cd ${singleQuote(workingDirectory)} && ( $command ) 2>&1"
    } else {
        "( $command ) 2>&1"
    }

    val pipe = popen(prefixed, "r") ?: throw RuntimeException("failed to start command: $command")
    val output = StringBuilder()
    memScoped {
        val bufferSize = 8192
        val buffer = allocArray<ByteVar>(bufferSize)
        while (true) {
            val chunk = fgets(buffer, bufferSize, pipe) ?: break
            output.append(chunk.toKString())
        }
    }
    val status = pclose(pipe)
    val exitCode = (status shr 8) and 0xFF
    return ShellResult(exitCode, output.toString())
}

/** Reads an environment variable, or null if unset. */
@OptIn(ExperimentalForeignApi::class)
fun environmentVariable(name: String): String? = getenv(name)?.toKString()

/** True if [name] resolves on PATH (or as a relative executable from [workingDirectory]). */
fun commandIsAvailable(name: String, workingDirectory: String): Boolean =
    runShellCommand("command -v ${singleQuote(name)} >/dev/null 2>&1", workingDirectory).exitCode == 0

/** Single-quotes a value for safe embedding in a shell command. */
fun singleQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
