package model

/** A single ordered step in a release procedure's `Run these steps sequentially` list. */
sealed interface ProcedureStep {
    /** `back up to file <path>` — back up the project; the location is remembered for restore. */
    data class BackUpToFile(val path: String) : ProcedureStep

    /** `restore the repository` — restore from the remembered backup. */
    data object RestoreRepository : ProcedureStep

    /** A bare bracketed callout, e.g. `[Build the signed, notarized DMG]`. */
    data class Callout(val name: String) : ProcedureStep
}
