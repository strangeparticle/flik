package com.strangeparticle.flik.program

/**
 * Aggregates the prerequisites across **all** pages in a linked program and checks them
 * against the local machine, returning **every** unmet one (not just the first), each
 * tagged with the pages that require it. Env/command checks are injected for testing;
 * the CLI binds them to the real environment and PATH (with the project root as cwd).
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
