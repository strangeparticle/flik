package com.strangeparticle.flik.command.util

/** One poll attempt's result: the command's combined output and its exit status. */
data class PollAttempt(val exitCode: Int, val output: String)
