package com.strangeparticle.flik.command.util

/**
 * Re-runs [runAttempt] until [matches] accepts its output, or [timeoutMillis] elapses.
 * Time and waiting are injected ([elapsedMillis], [sleep]) so the loop is testable without
 * a real clock. The command runs at least once before any timeout check; after a non-match
 * the elapsed time is checked, and only if there is still budget does it [sleep] and retry.
 * Exit codes are not consulted — only [matches] on the output decides success.
 */
fun pollUntilMatch(
    intervalMillis: Long,
    timeoutMillis: Long,
    runAttempt: () -> PollAttempt,
    matches: (output: String) -> Boolean,
    elapsedMillis: () -> Long,
    sleep: (millis: Long) -> Unit,
): PollOutcome {
    var attempts = 0
    while (true) {
        attempts++
        val attempt = runAttempt()
        if (matches(attempt.output)) {
            return PollOutcome.Matched(attempt.output, attempts)
        }
        if (elapsedMillis() >= timeoutMillis) {
            return PollOutcome.TimedOut(attempts, elapsedMillis(), attempt)
        }
        sleep(intervalMillis)
    }
}
