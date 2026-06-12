package com.strangeparticle.flik.program

import com.strangeparticle.flik.command.Page

/**
 * The linked whole-program: every reachable page parsed once (keyed by file path), the
 * invocation edges between them, and any structural diagnostics found while linking.
 */
data class CompileResult(
    val rootPath: String,
    val pages: Map<String, Page>,
    val edges: Map<String, List<String>>,
    val diagnostics: List<Diagnostic>,
)
