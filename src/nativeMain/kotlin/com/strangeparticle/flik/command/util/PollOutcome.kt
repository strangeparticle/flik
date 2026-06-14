package com.strangeparticle.flik.command.util

/** The result of polling: either an attempt matched, or the timeout elapsed first. */
sealed interface PollOutcome {
    /** The example was found. [output] is the matching attempt's output; [attempts] counts all attempts made. */
    data class Matched(val output: String, val attempts: Int) : PollOutcome

    /** The timeout elapsed with no match. Carries diagnostics for a clear failure message. */
    data class TimedOut(
        val attempts: Int,
        val elapsedMillis: Long,
        val lastAttempt: PollAttempt?,
    ) : PollOutcome
}
