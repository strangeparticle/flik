package run

import model.Check
import model.EntryDocument
import model.ProcedureStep
import model.StageCommand
import os.commandIsAvailable
import os.environmentVariable
import os.fileExists
import os.joinPath
import os.parentDirectoryOf
import os.readFileText
import os.runShellCommand
import os.singleQuote
import os.writeFileText
import parse.calloutFileName
import parse.parseSubDocument

/** Raised when a step fails; aborts the run (fail-on-error). */
class FlikExecutionException(message: String) : Exception(message)

/**
 * Executes an entry document: checks prerequisites, then runs each procedure step
 * in order, descending into callout sub-documents. Stops on the first failure.
 */
class Interpreter(
    private val context: ExecutionContext,
    private val entryDirectory: String,
    private val log: (String) -> Unit,
) {
    fun execute(document: EntryDocument) {
        checkPrerequisites(document)
        for (step in document.procedure) {
            executeStep(step)
        }
    }

    private fun checkPrerequisites(document: EntryDocument) {
        for (name in document.requiredEnvironmentVariables) {
            if (environmentVariable(name).isNullOrEmpty()) {
                throw FlikExecutionException("required environment variable not set: $name")
            }
        }
        for (command in document.requiredShellCommands) {
            if (!commandIsAvailable(command, context.projectRoot)) {
                throw FlikExecutionException("required command not found: $command")
            }
        }
        log(
            "prerequisites satisfied " +
                "(${document.requiredEnvironmentVariables.size} env vars, " +
                "${document.requiredShellCommands.size} commands)",
        )
    }

    private fun executeStep(step: ProcedureStep) {
        when (step) {
            is ProcedureStep.BackUpToFile -> backUp(step.path)
            is ProcedureStep.RestoreRepository -> restore()
            is ProcedureStep.Callout -> runCallout(step.name)
        }
    }

    private fun backUp(path: String) {
        log("backing up project to $path")
        val result = runShellCommand(
            "tar czf ${singleQuote(path)} --exclude=./build --exclude=./desktopApp/build .",
            context.projectRoot,
        )
        if (result.exitCode != 0) throw FlikExecutionException("backup failed:\n${result.output}")
        context.backupPath = path
    }

    private fun restore() {
        val path = context.backupPath
            ?: throw FlikExecutionException("restore requested but no backup was taken")
        log("restoring project from $path")
        val result = runShellCommand("tar xzf ${singleQuote(path)}", context.projectRoot)
        if (result.exitCode != 0) throw FlikExecutionException("restore failed:\n${result.output}")
    }

    private fun runCallout(name: String) {
        val fileName = calloutFileName(name)
        val path = joinPath(entryDirectory, fileName)
        if (!fileExists(path)) throw FlikExecutionException("callout \"$name\" -> $fileName not found")
        log("── $name")
        val subDocument = parseSubDocument(readFileText(path))
        val subDirectory = parentDirectoryOf(path)
        for (command in subDocument.commands) {
            executeStageCommand(command, subDirectory)
        }
    }

    private fun executeStageCommand(command: StageCommand, subDirectory: String) {
        when (command) {
            is StageCommand.CopyFile -> copyFile(command.from, command.to, subDirectory)
            is StageCommand.RunCommand -> runCommand(command.command)
            is StageCommand.ExpectFileExists -> expectFileExists(command.path)
            is StageCommand.EditInPlace -> editInPlace(command.file, command.find, command.replace)
            is StageCommand.ParallelChecks -> runChecks(command.checks)
        }
    }

    private fun copyFile(from: String, to: String, subDirectory: String) {
        val source = if (from.startsWith("/")) from else joinPath(subDirectory, from)
        val destination = resolveAgainstProjectRoot(to)
        val destinationDirectory = parentDirectoryOf(destination)
        log("  copy $from -> $to")
        val result = runShellCommand(
            "mkdir -p ${singleQuote(destinationDirectory)} && " +
                "cp ${singleQuote(source)} ${singleQuote(destination)}",
        )
        if (result.exitCode != 0) {
            throw FlikExecutionException("copy failed ($from -> $to):\n${result.output}")
        }
    }

    private fun runCommand(rawCommand: String) {
        val command = interpolate(rawCommand, context.projectRoot)
        log("  run: ${firstLine(command)}")
        val result = runShellCommand(command, context.projectRoot)
        if (result.exitCode != 0) {
            throw FlikExecutionException(
                "command failed (exit ${result.exitCode}): ${firstLine(command)}\n${result.output}",
            )
        }
    }

    private fun expectFileExists(rawPath: String) {
        val path = resolveAgainstProjectRoot(rawPath)
        log("  expect file: $path")
        if (!fileExists(path)) throw FlikExecutionException("expected file to exist: $path")
    }

    private fun editInPlace(file: String, find: String, replace: String) {
        val path = resolveAgainstProjectRoot(file)
        log("  edit: $file")
        if (!fileExists(path)) throw FlikExecutionException("file to edit not found: $path")
        val original = readFileText(path)
        val findBlock = interpolate(find, context.projectRoot)
        val replaceBlock = interpolate(replace, context.projectRoot)
        if (!original.contains(findBlock)) {
            throw FlikExecutionException("find block not found in $file")
        }
        writeFileText(path, original.replaceFirst(findBlock, replaceBlock))
    }

    private fun runChecks(checks: List<Check>) {
        log("  ${checks.size} checks:")
        for (check in checks) {
            val command = interpolate(check.command, context.projectRoot)
            val result = runShellCommand(command, context.projectRoot)
            if (result.exitCode != 0) {
                throw FlikExecutionException("check failed: ${firstLine(check.description)}\n${result.output}")
            }
            val expected = check.expectContains
            if (expected != null && !result.output.contains(interpolate(expected, context.projectRoot))) {
                throw FlikExecutionException(
                    "check output did not contain \"$expected\": ${firstLine(check.description)}",
                )
            }
            log("    ✓ ${firstLine(check.description)}")
        }
    }

    private fun resolveAgainstProjectRoot(rawPath: String): String {
        val path = interpolate(rawPath, context.projectRoot)
        return if (path.startsWith("/")) path else joinPath(context.projectRoot, path)
    }

    private fun firstLine(text: String): String {
        val line = text.lineSequence().firstOrNull()?.trim().orEmpty()
        return if (line.length > 100) line.take(99) + "…" else line
    }
}
