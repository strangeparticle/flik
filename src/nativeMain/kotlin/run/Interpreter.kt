package run

import command.ExecutionContext
import command.FlikExecutionException
import model.EntryDocument
import os.commandIsAvailable
import os.environmentVariable

/**
 * Drives a `flik run`: checks prerequisites, then executes each procedure step in
 * order. The per-command behavior lives in the `command` package; this is a thin
 * driver. Fail-on-error: any thrown [FlikExecutionException] aborts the run.
 */
class Interpreter(private val context: ExecutionContext) {
    fun execute(document: EntryDocument) {
        checkPrerequisites(document)
        for (step in document.procedure) {
            step.execute(context)
        }
    }

    private fun checkPrerequisites(document: EntryDocument) {
        for (name in document.requiredEnvironmentVariables) {
            if (environmentVariable(name).isNullOrEmpty()) {
                throw FlikExecutionException("required environment variable not set: $name")
            }
        }
        for (commandName in document.requiredShellCommands) {
            if (!commandIsAvailable(commandName, context.projectRoot)) {
                throw FlikExecutionException("required command not found: $commandName")
            }
        }
        context.log(
            "prerequisites satisfied " +
                "(${document.requiredEnvironmentVariables.size} env vars, " +
                "${document.requiredShellCommands.size} commands)",
        )
    }
}
