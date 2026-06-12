package command

import os.runShellCommand
import os.singleQuote

/** `restore the repository` — restore from the backup remembered by `back up to file`. */
class RestoreStep : ProcedureStep {
    override fun execute(context: ExecutionContext) {
        val path = context.backupPath
            ?: throw FlikExecutionException("restore requested but no backup was taken")
        context.log("restoring project from $path")
        val result = runShellCommand("tar xzf ${singleQuote(path)}", context.projectRoot)
        if (result.exitCode != 0) throw FlikExecutionException("restore failed:\n${result.output}")
    }

    companion object : ProcedureStepParser {
        override fun tryParse(bullet: String): ProcedureStep? =
            if (bullet.equals("restore the repository", ignoreCase = true)) RestoreStep() else null
    }
}
