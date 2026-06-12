package command

import os.joinPath

/** Mutable state shared across a single `flik run`. */
class ExecutionContext(
    val projectRoot: String,
    val entryDirectory: String,
    val log: (String) -> Unit,
) {
    /** The remembered backup location: set by `back up to file`, used by `restore the repository`. */
    var backupPath: String? = null

    /** Directory of the stage document currently executing, for resolving its relative sources. */
    var stageDirectory: String? = null

    /** Resolves a possibly-interpolated path against the project root (absolute paths pass through). */
    fun resolveAgainstProjectRoot(rawPath: String): String {
        val path = interpolate(rawPath, projectRoot)
        return if (path.startsWith("/")) path else joinPath(projectRoot, path)
    }
}
