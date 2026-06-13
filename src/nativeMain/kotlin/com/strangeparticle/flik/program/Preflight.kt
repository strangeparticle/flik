package com.strangeparticle.flik.program

import com.strangeparticle.flik.command.util.environmentReferences

/**
 * Aggregates the prerequisites across **all** pages in a linked program and checks them
 * against the local machine, returning **every** unmet one (not just the first), each
 * tagged with the pages that require it. Env/command checks are injected for testing;
 * the CLI binds them to the real environment and PATH (with the project root as cwd).
 *
 * Two sources of required environment variables are checked together:
 *  - explicit `* environment variable: \`NAME\`` prerequisites, and
 *  - implicit references via `${{ env.NAME }}` interpolation anywhere in a page (a path,
 *    a run command, an edit block) — discovered by scanning each page's source.
 * Both feed the same "environment variable not set" report, so an author who writes
 * `${{ env.FOO }}` without declaring FOO as a prerequisite still gets a clear, up-front
 * error rather than the run aborting on the interpolation backstop mid-execution.
 */
fun preflight(
    result: CompileResult,
    environmentVariable: (String) -> String?,
    commandAvailable: (String) -> Boolean,
): List<String> {
    val environmentToPages = LinkedHashMap<String, MutableList<String>>()
    val commandToPages = LinkedHashMap<String, MutableList<String>>()

    for ((path, page) in result.pages) {
        val pageName = path.substringAfterLast('/')
        for (variable in page.requiredEnvironmentVariables) {
            environmentToPages.getOrPut(variable) { mutableListOf() }.add(pageName)
        }
        for (variable in environmentReferences(page.source)) {
            val requiringPages = environmentToPages.getOrPut(variable) { mutableListOf() }
            if (pageName !in requiringPages) {
                requiringPages.add(pageName)
            }
        }
        for (command in page.requiredShellCommands) {
            commandToPages.getOrPut(command) { mutableListOf() }.add(pageName)
        }
    }

    val problems = mutableListOf<String>()
    for ((variable, requiringPages) in environmentToPages) {
        if (environmentVariable(variable).isNullOrEmpty()) {
            problems += "environment variable not set: $variable (required by ${requiringPages.joinToString(", ")})"
        }
    }
    for ((command, requiringPages) in commandToPages) {
        if (!commandAvailable(command)) {
            problems += "command not found: $command (required by ${requiringPages.joinToString(", ")})"
        }
    }
    return problems
}
