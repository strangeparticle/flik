package model

/** A command inside a stage (sub-document). */
sealed interface StageCommand {
    /** `Copy file from: <from> to: <to>` — `from` is relative to the sub-document; `to` to the project root. */
    data class CopyFile(val from: String, val to: String) : StageCommand

    /** `Run command:` + a fenced shell block. Fails on a non-zero exit. */
    data class RunCommand(val command: String) : StageCommand

    /** `Expect file to exist: <path>`. */
    data class ExpectFileExists(val path: String) : StageCommand

    /** `In file: <file>` / `Find this section:` / `Replace it with:` — an in-place edit. */
    data class EditInPlace(val file: String, val find: String, val replace: String) : StageCommand

    /** `Run these checks in parallel:` + a bullet list of [Check]s. */
    data class ParallelChecks(val checks: List<Check>) : StageCommand
}

/** One parallel verification check: a command and an optional substring its output must contain. */
data class Check(val description: String, val command: String, val expectContains: String?)
