import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import os.fileExists
import os.joinPath
import os.parentDirectoryOf
import os.readFileText
import parse.FlikParseException
import parse.parseEntryDocument
import validate.findEntryDocumentProblems

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
    private val file: String by argument(name = "file", help = "Path to the entry .flik.md document")

    override fun run() {
        val source = try {
            readFileText(file)
        } catch (failure: Exception) {
            echo("cannot read $file: ${failure.message}", err = true)
            throw ProgramResult(1)
        }

        val document = try {
            parseEntryDocument(source)
        } catch (failure: FlikParseException) {
            echo("parse error in $file: ${failure.message}", err = true)
            throw ProgramResult(1)
        }

        val directory = parentDirectoryOf(file)
        val problems = findEntryDocumentProblems(document) { fileName ->
            fileExists(joinPath(directory, fileName))
        }

        if (problems.isEmpty()) {
            echo("OK: ${document.title}")
            echo(
                "  ${document.procedure.size} steps, " +
                    "${document.requiredEnvironmentVariables.size} required env vars, " +
                    "${document.requiredShellCommands.size} required commands",
            )
        } else {
            echo("problems in $file:", err = true)
            problems.forEach { echo("  - $it", err = true) }
            throw ProgramResult(1)
        }
    }
}

fun main(args: Array<String>) =
    Flik().subcommands(Version(), Validate()).main(args)
