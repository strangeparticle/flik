package com.strangeparticle.flik

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.strangeparticle.flik.command.FlikExecutionException
import com.strangeparticle.flik.os.commandIsAvailable
import com.strangeparticle.flik.os.environmentVariable
import com.strangeparticle.flik.os.fileExists
import com.strangeparticle.flik.os.isDirectory
import com.strangeparticle.flik.os.readFileText
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.program.CompileResult
import com.strangeparticle.flik.program.RootPageResolution
import com.strangeparticle.flik.program.compile
import com.strangeparticle.flik.program.preflight
import com.strangeparticle.flik.program.resolveRootPage
import com.strangeparticle.flik.run.Interpreter

class Flik : CliktCommand(name = "flik") {
    init {
        versionOption(FlikVersion.VERSION)
    }

    override fun run() = Unit
}

class Version : CliktCommand(name = "version") {
    override fun run() {
        echo("flik version ${FlikVersion.VERSION}")
    }
}

/** Links the whole page graph from [file], reporting structural problems (no execution). */
class Validate : CliktCommand(name = "validate") {
    private val file: String by argument(name = "file", help = "A root .flik.md page, or a folder containing one")

    override fun run() {
        val program = compile(resolveOrExit(file), ::readFileText, ::fileExists)
        if (program.diagnostics.isNotEmpty()) {
            reportDiagnostics(program)
            throw ProgramResult(1)
        }
        echo("OK: ${program.pages[program.rootPath]?.title ?: file}")
        echo("  ${program.pages.size} pages linked, no problems")
    }
}

/** Links, preflights all prerequisites at once, then executes the compiled program. */
class Run : CliktCommand(name = "run") {
    private val file: String by argument(name = "file", help = "A root .flik.md page, or a folder containing one")
    private val projectRoot: String by option(
        "--project-root",
        help = "Root of the project Flik operates on (default: current directory)",
    ).default(".")

    override fun run() {
        val program = compile(resolveOrExit(file), ::readFileText, ::fileExists)
        if (program.diagnostics.isNotEmpty()) {
            reportDiagnostics(program)
            throw ProgramResult(1)
        }

        val absoluteProjectRoot = runShellCommand("pwd", projectRoot).output.trim()
        val unmet = preflight(program, ::environmentVariable) { command ->
            commandIsAvailable(command, absoluteProjectRoot)
        }
        if (unmet.isNotEmpty()) {
            echo("unmet prerequisites:", err = true)
            unmet.forEach { echo("  - $it", err = true) }
            throw ProgramResult(1)
        }

        echo("Running: ${program.pages[program.rootPath]?.title}")
        echo("Project root: $absoluteProjectRoot")
        try {
            Interpreter(absoluteProjectRoot, program) { line -> echo(line) }.execute()
        } catch (failure: FlikExecutionException) {
            echo("", err = true)
            echo("FAILED: ${failure.message}", err = true)
            throw ProgramResult(1)
        }
        echo("Done.")
    }
}

/** Links, then prints the page graph, aggregated prerequisites, and structure (no execution). */
class Explain : CliktCommand(name = "explain") {
    private val file: String by argument(name = "file", help = "A root .flik.md page, or a folder containing one")

    override fun run() {
        val program = compile(resolveOrExit(file), ::readFileText, ::fileExists)
        if (program.diagnostics.isNotEmpty()) {
            reportDiagnostics(program)
            throw ProgramResult(1)
        }

        echo("Program: ${program.pages[program.rootPath]?.title}")
        echo("Pages (${program.pages.size}):")

        fun printTree(path: String, indent: String) {
            val page = program.pages[path] ?: return
            echo("$indent${page.title}  [${path.substringAfterLast('/')}]")
            program.edges[path].orEmpty().forEach { target -> printTree(target, "$indent  ") }
        }
        printTree(program.rootPath, "  ")

        val environmentVariables = program.pages.values.flatMap { it.requiredEnvironmentVariables }.distinct()
        val commands = program.pages.values.flatMap { it.requiredShellCommands }.distinct()
        if (environmentVariables.isNotEmpty()) {
            echo("Required environment variables: ${environmentVariables.joinToString(", ")}")
        }
        if (commands.isNotEmpty()) {
            echo("Required commands: ${commands.joinToString(", ")}")
        }
    }
}

/** Resolves a target (page or folder) to a root page path, or exits with a message. */
private fun CliktCommand.resolveOrExit(target: String): String =
    when (val resolution = resolveRootPage(target, ::isDirectory, ::fileExists)) {
        is RootPageResolution.Found -> resolution.path
        is RootPageResolution.NotFound -> {
            echo(resolution.message, err = true)
            throw ProgramResult(1)
        }
    }

private fun CliktCommand.reportDiagnostics(program: CompileResult) {
    echo("problems in the page graph:", err = true)
    program.diagnostics.forEach { diagnostic ->
        echo("  - [${diagnostic.page.substringAfterLast('/')}] ${diagnostic.message}", err = true)
    }
}

fun main(args: Array<String>) =
    Flik().subcommands(Version(), Validate(), Run(), Explain()).main(args)
