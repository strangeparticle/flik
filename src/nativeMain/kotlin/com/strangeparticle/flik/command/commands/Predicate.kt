package com.strangeparticle.flik.command.commands

import com.strangeparticle.flik.command.ExecutionContext
import com.strangeparticle.flik.os.commandIsAvailable
import com.strangeparticle.flik.os.fileExists

/**
 * A deterministic, machine-checkable condition for an `If <predicate>:` block. The set
 * is intentionally small and fixed — no boolean algebra, no expression language. The
 * prose around it is the human label; the backtick-quoted parts are what's parsed.
 */
sealed interface Predicate {
    fun evaluate(context: ExecutionContext): Boolean

    companion object {
        private val COMMAND_AVAILABLE = Regex("the command `(.+?)` is available", RegexOption.IGNORE_CASE)
        private val FILE_EXISTS = Regex("the file `(.+?)` exists", RegexOption.IGNORE_CASE)
        private val VALUE_EQUALS = Regex("`(.+?)` is `(.+?)`", RegexOption.IGNORE_CASE)

        /** Parses a condition (the text between `If` and the trailing `:`), or null if unrecognized. */
        fun parse(condition: String): Predicate? {
            val text = condition.trim()
            COMMAND_AVAILABLE.matchEntire(text)?.let { return CommandAvailable(it.groupValues[1]) }
            FILE_EXISTS.matchEntire(text)?.let { return FileExists(it.groupValues[1]) }
            VALUE_EQUALS.matchEntire(text)?.let { return ValueEquals(it.groupValues[1], it.groupValues[2]) }
            return null
        }
    }
}

/** `the command \`X\` is available` */
class CommandAvailable(val command: String) : Predicate {
    override fun evaluate(context: ExecutionContext): Boolean =
        commandIsAvailable(command, context.projectRoot)
}

/** `the file \`X\` exists` — path interpolated and resolved against the project root. */
class FileExists(val path: String) : Predicate {
    override fun evaluate(context: ExecutionContext): Boolean =
        fileExists(context.resolveAgainstProjectRoot(path))
}

/** `\`<value>\` is \`<literal>\`` — value interpolated, compared by exact trimmed equality. */
class ValueEquals(val value: String, val literal: String) : Predicate {
    override fun evaluate(context: ExecutionContext): Boolean =
        context.interpolate(value).trim() == literal
}
