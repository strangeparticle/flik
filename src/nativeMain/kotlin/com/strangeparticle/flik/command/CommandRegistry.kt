package com.strangeparticle.flik.command

import com.strangeparticle.flik.command.commands.AppendToFileCommand
import com.strangeparticle.flik.command.commands.BackUpCommand
import com.strangeparticle.flik.command.commands.ConditionalCommand
import com.strangeparticle.flik.command.commands.CopyFileCommand
import com.strangeparticle.flik.command.commands.CreateOrAppendToFileCommand
import com.strangeparticle.flik.command.commands.EditInPlaceCommand
import com.strangeparticle.flik.command.commands.ExpectFileCommand
import com.strangeparticle.flik.command.commands.RestoreCommand
import com.strangeparticle.flik.command.commands.RunChecksCommand
import com.strangeparticle.flik.command.commands.RunCommand
import com.strangeparticle.flik.command.commands.SwitchCommand
import com.strangeparticle.flik.command.commands.WriteToFileCommand

/**
 * The element registry. Adding a new command means adding one file in this package
 * and one entry here — the parser loop and the page runner need no changes.
 */
val pageElementParsers: List<PageElementParser> = listOf(
    ConditionalCommand,
    SwitchCommand,
    CopyFileCommand,
    WriteToFileCommand,
    AppendToFileCommand,
    CreateOrAppendToFileCommand,
    RunCommand,
    ExpectFileCommand,
    EditInPlaceCommand,
    RunChecksCommand,
    BackUpCommand,
    RestoreCommand,
    PageInvocation,
)

/**
 * Parses exactly one element beginning at `lines[index]`, or null if nothing matches.
 * The branching commands ([ConditionalCommand], [SwitchCommand]) use this to parse their
 * branch elements, so a branch can be any single element the registry recognizes.
 */
fun parseOneElement(lines: List<String>, index: Int): ParsedElement? =
    pageElementParsers.firstNotNullOfOrNull { it.tryParse(lines, index) }
