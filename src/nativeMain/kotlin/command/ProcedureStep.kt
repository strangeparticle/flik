package command

/** A parsed, executable step in the entry document's release procedure. */
interface ProcedureStep {
    fun execute(context: ExecutionContext)
}

/** Recognizes one procedure step from a single procedure-bullet's text, or returns null. */
interface ProcedureStepParser {
    fun tryParse(bullet: String): ProcedureStep?
}
