package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.util.interpolate
import com.strangeparticle.flik.os.joinPath

/** A page currently on the execution stack. */
private data class PageFrame(val pageId: String, val pageDirectory: String)

/**
 * Mutable state shared across a single `flik run`. Tracks the page-invocation stack
 * (so commands resolve paths relative to the page that contains them and backups are
 * attributed to that page) and the backups taken so far.
 */
class ExecutionContext(
    val projectRoot: String,
    val backupsDirectory: String,
    val log: (String) -> Unit,
) {
    private val frames = ArrayDeque<PageFrame>()
    private val backups = mutableListOf<BackupRecord>()

    /** Identity (file path) of the page currently executing. */
    val currentPageId: String get() = frames.last().pageId

    /** Directory of the page currently executing, for resolving its relative resources. */
    val currentPageDirectory: String get() = frames.last().pageDirectory

    /** Runs [block] with [pageId]/[pageDirectory] pushed as the current page, popping afterward. */
    fun <T> withPage(pageId: String, pageDirectory: String, block: () -> T): T {
        frames.addLast(PageFrame(pageId, pageDirectory))
        try {
            return block()
        } finally {
            frames.removeLast()
        }
    }

    /** The absolute path a backup named [fileName] is written to (under the run's backups dir). */
    fun backupPathFor(fileName: String): String = joinPath(backupsDirectory, fileName)

    fun recordBackup(fileName: String, filePath: String) {
        backups.add(BackupRecord(currentPageId, fileName, filePath))
    }

    /** The most recent backup taken by the page currently executing, or null. */
    fun mostRecentBackupForCurrentPage(): BackupRecord? =
        backups.lastOrNull { it.pageId == currentPageId }

    /** Resolves a possibly-interpolated path against the project root (absolute paths pass through). */
    fun resolveAgainstProjectRoot(rawPath: String): String {
        val path = interpolate(rawPath, projectRoot)
        return if (path.startsWith("/")) path else joinPath(projectRoot, path)
    }
}
