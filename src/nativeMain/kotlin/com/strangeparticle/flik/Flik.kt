package com.strangeparticle.flik

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.command.parsePage
import com.strangeparticle.flik.os.fileExists
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.os.readFileText
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.parse.FlikParseException
import com.strangeparticle.flik.run.Interpreter
import com.strangeparticle.flik.validate.findPageProblems

const val FLIK_VERSION = "0.1.0"

class Flik : CliktCommand(name = "flik") {
    override fun run() = Unit
}

class Version : CliktCommand(name = "version") {
    override fun run() {
        echo("flik $FLIK_VERSION")
    }
}

class Validate : CliktCommand(name = "validate") {
    private val file: String by argument(name = "file", help = "Path to the .flik.md page")

    override fun run() {
        val page = try {
            parsePage(readFileTextOrExit(file))
        } catch (failure: FlikParseException) {
            echo("parse error in $file: ${failure.message}", err = true)
            throw ProgramResult(1)
        }

        val directory = parentDirectoryOf(file)
        val problems = findPageProblems(page) { fileName -> fileExists(joinPath(directory, fileName)) }

        if (problems.isEmpty()) {
            echo("OK: ${page.title}")
            echo(
                "  ${page.elements.size} elements, " +
                    "${page.requiredEnvironmentVariables.size} required env vars, " +
                    "${page.requiredShellCommands.size} required commands",
            )
        } else {
            echo("problems in $file:", err = true)
            problems.forEach { echo("  - $it", err = true) }
            throw ProgramResult(1)
        }
    }
}

class Run : CliktCommand(name = "run") {
    private val file: String by argument(name = "file", help = "Path to the root .flik.md page")
    private val projectRoot: String by option(
        "--project-root",
        help = "Root of the project Flik operates on (default: current directory)",
    ).default(".")

    override fun run() {
        val page = try {
            parsePage(readFileTextOrExit(file))
        } catch (failure: FlikParseException) {
            echo("parse error in $file: ${failure.message}", err = true)
            throw ProgramResult(1)
        }

        val absoluteProjectRoot = runShellCommand("pwd", projectRoot).output.trim()
        echo("Running: ${page.title}")
        echo("Project root: $absoluteProjectRoot")

        val interpreter = Interpreter(absoluteProjectRoot, file) { line -> echo(line) }
        try {
            interpreter.execute(page)
        } catch (failure: FlikExecutionException) {
            echo("", err = true)
            echo("FAILED: ${failure.message}", err = true)
            throw ProgramResult(1)
        }
        echo("Done.")
    }
}

private fun CliktCommand.readFileTextOrExit(file: String): String =
    try {
        readFileText(file)
    } catch (failure: Exception) {
        echo("cannot read $file: ${failure.message}", err = true)
        throw ProgramResult(1)
    }

fun main(args: Array<String>) =
    Flik().subcommands(Version(), Validate(), Run()).main(args)
