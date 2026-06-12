package com.strangeparticle.flik.run

import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.runPage
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.singleQuote
import com.strangeparticle.flik.program.CompileResult

/**
 * Drives a `flik run` over an already-linked program: creates a timestamped execution
 * directory beside the root page (`executions/<timestamp>/backups/`), builds the
 * execution context (carrying the compiled pages), and runs the root page. Callouts
 * resolve to pages already parsed during linking, so no page is parsed twice.
 */
class Interpreter(
    private val projectRoot: String,
    private val program: CompileResult,
    private val log: (String) -> Unit,
) {
    fun execute() {
        val rootPath = program.rootPath
        val rootPage = program.pages[rootPath]
            ?: error("root page $rootPath missing from the compiled program")
        val rootPageDirectory = parentDirectoryOf(rootPath)

        val timestamp = runShellCommand("date -u +%Y-%m-%dT%H-%M-%SZ").output.trim()
        val executionDirectory = joinPath(joinPath(rootPageDirectory, "executions"), timestamp)
        val backupsDirectory = joinPath(executionDirectory, "backups")
        runShellCommand("mkdir -p ${singleQuote(backupsDirectory)}")
        log("Execution dir: $executionDirectory")

        val context = ExecutionContext(projectRoot, backupsDirectory, program.pages, log)
        runPage(rootPage, pageId = rootPath, pageDirectory = rootPageDirectory, context = context)
    }
}
