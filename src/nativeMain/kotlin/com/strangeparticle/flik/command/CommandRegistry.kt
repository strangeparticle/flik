package com.strangeparticle.flik.command

/**
 * The element registry. Adding a new command means adding one file in this package
 * and one entry here — the parser loop and the page runner need no changes.
 */
val pageElementParsers: List<PageElementParser> = listOf(
    CopyFileCommand,
    RunShellCommand,
    ExpectFileCommand,
    EditInPlaceCommand,
    RunChecksCommand,
    BackUpCommand,
    RestoreCommand,
    PageInvocation,
)
