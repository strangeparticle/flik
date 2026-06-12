package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.commands.BackUpCommand
import com.strangeparticle.flik.command.commands.CopyFileCommand
import com.strangeparticle.flik.command.commands.EditInPlaceCommand
import com.strangeparticle.flik.command.commands.ExpectFileCommand
import com.strangeparticle.flik.command.commands.ExpectToSeeCommand
import com.strangeparticle.flik.command.commands.RestoreCommand
import com.strangeparticle.flik.command.commands.RunChecksCommand
import com.strangeparticle.flik.command.commands.RunShellCommand

/**
 * The element registry. Adding a new command means adding one file in this package
 * and one entry here — the parser loop and the page runner need no changes.
 */
val pageElementParsers: List<PageElementParser> = listOf(
    CopyFileCommand,
    RunShellCommand,
    ExpectFileCommand,
    ExpectToSeeCommand,
    EditInPlaceCommand,
    RunChecksCommand,
    BackUpCommand,
    RestoreCommand,
    PageInvocation,
)
