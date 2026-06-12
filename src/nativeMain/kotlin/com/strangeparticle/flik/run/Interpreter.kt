package com.strangeparticle.flik.run

import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.command.Page
import com.strangeparticle.flik.command.runPage
import com.strangeparticle.flik.os.joinPath
import com.strangeparticle.flik.os.parentDirectoryOf
import com.strangeparticle.flik.os.runShellCommand
import com.strangeparticle.flik.os.singleQuote

/**
 * Drives a `flik run`: creates a timestamped execution directory beside the root page
 * (`executions/<timestamp>/backups/`), builds the execution context, and runs the
 * root page. The root page is run with the same machinery as any invoked page.
 */
class Interpreter(
    private val projectRoot: String,
    private val rootPagePath: String,
    private val log: (String) -> Unit,
) {
    fun execute(page: Page) {
        val rootPageDirectory = parentDirectoryOf(rootPagePath)
        val timestamp = runShellCommand("date -u +%Y-%m-%dT%H-%M-%SZ").output.trim()
        val executionDirectory = joinPath(joinPath(rootPageDirectory, "executions"), timestamp)
        val backupsDirectory = joinPath(executionDirectory, "backups")
        runShellCommand("mkdir -p ${singleQuote(backupsDirectory)}")
        log("Execution dir: $executionDirectory")

        val context = ExecutionContext(projectRoot, backupsDirectory, log)
        runPage(page, pageId = rootPagePath, pageDirectory = rootPageDirectory, context = context)
    }
}
