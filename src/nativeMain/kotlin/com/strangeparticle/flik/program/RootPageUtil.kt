package com.strangeparticle.flik.program

import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.parse.calloutFileName

/** The outcome of resolving a CLI target to a root page. */
sealed interface RootPageResolution {
    data class Found(val path: String) : RootPageResolution
    data class NotFound(val message: String) : RootPageResolution
}

/**
 * Resolves a CLI target to a root page path. A **directory** resolves to its
 * `<folder-name>.flik.md` — the folder name normalized exactly the way callouts are.
 * A file path is used as-is (compile reports it if unreadable). Filesystem access is
 * injected for testing.
 */
fun resolveRootPage(
    target: String,
    isDirectory: (String) -> Boolean,
    fileExists: (String) -> Boolean,
): RootPageResolution {
    if (!isDirectory(target)) {
        return RootPageResolution.Found(target)
    }
    val directory = target.trimEnd('/')
    val rootFileName = calloutFileName(directory.substringAfterLast('/'))
    val candidate = joinPath(directory, rootFileName)
    return if (fileExists(candidate)) {
        RootPageResolution.Found(candidate)
    } else {
        RootPageResolution.NotFound("no root page in $target: expected $rootFileName")
    }
}
