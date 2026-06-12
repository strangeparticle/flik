package com.strangeparticle.flik.command

import com.strangeparticle.flik.os.commandIsAvailable
import com.strangeparticle.flik.os.environmentVariable

/**
 * Runs a page: pushes it as the current page, checks its prerequisites, then executes
 * its elements in order. Used for the root page and, recursively, for every invoked
 * page. Fail-on-error: a thrown [FlikExecutionException] aborts the run.
 */
fun runPage(page: Page, pageId: String, pageDirectory: String, context: ExecutionContext) {
    context.withPage(pageId, pageDirectory) {
        checkPrerequisites(page, context)
        for (element in page.elements) {
            element.execute(context)
        }
    }
}

private fun checkPrerequisites(page: Page, context: ExecutionContext) {
    for (name in page.requiredEnvironmentVariables) {
        if (environmentVariable(name).isNullOrEmpty()) {
            throw FlikExecutionException("required environment variable not set: $name")
        }
    }
    for (commandName in page.requiredShellCommands) {
        if (!commandIsAvailable(commandName, context.projectRoot)) {
            throw FlikExecutionException("required command not found: $commandName")
        }
    }
    if (page.requiredEnvironmentVariables.isNotEmpty() || page.requiredShellCommands.isNotEmpty()) {
        context.log(
            "prerequisites satisfied " +
                "(${page.requiredEnvironmentVariables.size} env vars, " +
                "${page.requiredShellCommands.size} commands)",
        )
    }
}
