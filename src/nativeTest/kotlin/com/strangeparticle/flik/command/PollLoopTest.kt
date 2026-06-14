package com.strangeparticle.flik.command.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PollLoopTest {
    private fun matchEquals(target: String): (String) -> Boolean = { output -> output == target }

    @Test
    fun matchesOnTheFirstAttemptWithoutSleeping() {
        var now = 0L
        var sleeps = 0
        val outcome = pollUntilMatch(
            intervalMillis = 5_000,
            timeoutMillis = 10_000,
            runAttempt = { PollAttempt(0, "done") },
            matches = matchEquals("done"),
            elapsedMillis = { now },
            sleep = { millis -> sleeps++; now += millis },
        )
        val matched = assertIs<PollOutcome.Matched>(outcome)
        assertEquals("done", matched.output)
        assertEquals(1, matched.attempts)
        assertEquals(0, sleeps)
    }

    @Test
    fun matchesAfterSeveralMisses() {
        var now = 0L
        val outputs = ArrayDeque(listOf("no", "no", "done"))
        val outcome = pollUntilMatch(
            intervalMillis = 1_000,
            timeoutMillis = 60_000,
            runAttempt = { PollAttempt(0, outputs.removeFirst()) },
            matches = matchEquals("done"),
            elapsedMillis = { now },
            sleep = { millis -> now += millis },
        )
        val matched = assertIs<PollOutcome.Matched>(outcome)
        assertEquals(3, matched.attempts)
    }

    @Test
    fun ignoresExitCodeWhenDecidingAMatch() {
        var now = 0L
        val attempts = ArrayDeque(listOf(PollAttempt(7, "no"), PollAttempt(0, "done")))
        val outcome = pollUntilMatch(
            intervalMillis = 1_000,
            timeoutMillis = 60_000,
            runAttempt = { attempts.removeFirst() },
            matches = matchEquals("done"),
            elapsedMillis = { now },
            sleep = { millis -> now += millis },
        )
        assertIs<PollOutcome.Matched>(outcome)
    }

    @Test
    fun timesOutWhenNeverMatching() {
        var now = 0L
        val outcome = pollUntilMatch(
            intervalMillis = 5_000,
            timeoutMillis = 10_000,
            runAttempt = { PollAttempt(1, "still working") },
            matches = matchEquals("done"),
            elapsedMillis = { now },
            sleep = { millis -> now += millis },
        )
        val timedOut = assertIs<PollOutcome.TimedOut>(outcome)
        assertEquals(3, timedOut.attempts)
        assertEquals(10_000, timedOut.elapsedMillis)
        assertEquals("still working", timedOut.lastAttempt?.output)
    }
}
