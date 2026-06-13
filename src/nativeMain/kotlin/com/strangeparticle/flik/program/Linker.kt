package com.strangeparticle.flik.program

import com.strangeparticle.flik.command.Page
import com.strangeparticle.flik.command.parsePage
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.parse.FlikParseException
import com.strangeparticle.flik.parse.calloutFileName

/**
 * Eagerly loads and links the whole page graph reachable from [rootPath], parsing each
 * page exactly once. Pure and machine-independent: file access is injected via
 * [readFile] and [fileExists], so it needs no env / PATH / project_root and can run in
 * CI. Collects ALL structural diagnostics (unreadable/missing pages, parse errors,
 * missing version footers, cycles) rather than stopping at the first.
 */
fun compile(
    rootPath: String,
    readFile: (String) -> String,
    fileExists: (String) -> Boolean,
): CompileResult {
    val pages = LinkedHashMap<String, Page>()
    val edges = LinkedHashMap<String, List<String>>()
    val diagnostics = mutableListOf<Diagnostic>()
    val onStack = mutableSetOf<String>()
    val visited = mutableSetOf<String>()

    fun visit(path: String) {
        if (!visited.add(path)) return
        onStack += path
        try {
            val source = try {
                readFile(path)
            } catch (failure: Exception) {
                diagnostics += Diagnostic(path, "cannot read page: ${failure.message}")
                return
            }
            val page = try {
                parsePage(source)
            } catch (failure: FlikParseException) {
                diagnostics += Diagnostic(path, "parse error: ${failure.message}")
                return
            }
            pages[path] = page
            if (page.flikVersion == null) {
                diagnostics += Diagnostic(path, "missing the Flik version footer")
            }

            val directory = parentDirectoryOf(path)
            val targets = mutableListOf<String>()
            for (element in page.elements) {
                for (invocation in element.referencedPageInvocations()) {
                    val fileName = calloutFileName(invocation.name)
                    val targetPath = joinPath(directory, fileName)
                    targets += targetPath
                    when {
                        !fileExists(targetPath) ->
                            diagnostics += Diagnostic(path, "page invocation \"${invocation.name}\" -> $fileName not found")
                        targetPath in onStack ->
                            diagnostics += Diagnostic(path, "cycle: \"${invocation.name}\" ($fileName) is already on the invocation path")
                        else -> visit(targetPath)
                    }
                }
            }
            edges[path] = targets
        } finally {
            onStack -= path
        }
    }

    visit(rootPath)
    return CompileResult(rootPath, pages, edges, diagnostics)
}
