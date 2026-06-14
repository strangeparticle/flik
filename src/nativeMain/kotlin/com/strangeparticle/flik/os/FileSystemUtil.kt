package com.strangeparticle.flik.os

import com.strangeparticle.flik.command.FlikExecutionException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import platform.posix.F_OK
import platform.posix.access
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.opendir

/** Reads an entire file as UTF-8 text. Throws if the file cannot be opened. */
@OptIn(ExperimentalForeignApi::class)
fun readFileText(path: String): String {
    val file = fopen(path, "rb") ?: throw RuntimeException("cannot open file: $path")
    try {
        val builder = StringBuilder()
        memScoped {
            val bufferSize = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferSize)
            while (true) {
                val read = fread(buffer, 1.convert(), bufferSize.convert(), file).toInt()
                if (read <= 0) break
                builder.append(buffer.readBytes(read).decodeToString())
            }
        }
        return builder.toString()
    } finally {
        fclose(file)
    }
}

/** Writes [text] to [path] as UTF-8, truncating any existing file. */
@OptIn(ExperimentalForeignApi::class)
fun writeFileText(path: String, text: String) {
    val file = fopen(path, "wb") ?: throw RuntimeException("cannot write file: $path")
    try {
        val bytes = text.encodeToByteArray()
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
            }
        }
    } finally {
        fclose(file)
    }
}

/** Appends [text] to [path] as UTF-8, creating the file if it does not exist. */
@OptIn(ExperimentalForeignApi::class)
fun appendFileText(path: String, text: String) {
    val file = fopen(path, "ab") ?: throw RuntimeException("cannot append to file: $path")
    try {
        val bytes = text.encodeToByteArray()
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
            }
        }
    } finally {
        fclose(file)
    }
}

/**
 * Creates [directory] and any missing parent directories (`mkdir -p` semantics). A no-op
 * when the directory already exists. Throws [FlikExecutionException] if creation fails.
 */
fun makeDirectoriesFor(directory: String) {
    if (directory == "." || isDirectory(directory)) return
    val result = runShellCommand("mkdir -p ${singleQuote(directory)}")
    if (result.exitCode != 0) {
        throw FlikExecutionException("cannot create directory $directory:\n${result.output}")
    }
}

/** True if a filesystem entry exists at [path]. */
@OptIn(ExperimentalForeignApi::class)
fun fileExists(path: String): Boolean = access(path, F_OK) == 0

/** True if [path] is a directory. */
@OptIn(ExperimentalForeignApi::class)
fun isDirectory(path: String): Boolean {
    val dir = opendir(path) ?: return false
    closedir(dir)
    return true
}

/** The directory containing [path], or "." when [path] has no directory component. */
fun parentDirectoryOf(path: String): String {
    val index = path.lastIndexOf('/')
    return if (index <= 0) "." else path.substring(0, index)
}

/** Joins a directory and a child name with a single separator. */
fun joinPath(directory: String, child: String): String =
    if (directory.endsWith("/")) "$directory$child" else "$directory/$child"
