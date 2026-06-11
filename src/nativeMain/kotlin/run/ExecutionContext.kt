package run

/** Mutable state shared across a single `flik run` execution. */
class ExecutionContext(val projectRoot: String) {
    /** The remembered backup location, set by `back up to file` and used by `restore the repository`. */
    var backupPath: String? = null
}
