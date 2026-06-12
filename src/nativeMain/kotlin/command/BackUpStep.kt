package command

import os.runShellCommand
import os.singleQuote
import parse.FlikParseException

/** `back up to file <path>` — back up the project (excluding build output); remembers the location. */
class BackUpStep(val path: String) : ProcedureStep {
    override fun execute(context: ExecutionContext) {
        context.log("backing up project to $path")
        val result = runShellCommand(
            "tar czf ${singleQuote(path)} --exclude=./build --exclude=./desktopApp/build .",
            context.projectRoot,
        )
        if (result.exitCode != 0) throw FlikExecutionException("backup failed:\n${result.output}")
        context.backupPath = path
    }

    companion object : ProcedureStepParser {
        private const val PREFIX = "back up to file"
        override fun tryParse(bullet: String): ProcedureStep? {
            if (!bullet.startsWith(PREFIX, ignoreCase = true)) return null
            val path = bullet.substring(PREFIX.length).trim().trim('`').trim()
            if (path.isEmpty()) throw FlikParseException("'$PREFIX' step has no path")
            return BackUpStep(path)
        }
    }
}
