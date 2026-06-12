package command

/** A parsed, executable stage command — one entry in a stage (sub-)document. */
interface StageStep {
    fun execute(context: ExecutionContext)
}

/** A successful parselet match: the parsed step and the line index just past it. */
data class ParsedStageStep(val step: StageStep, val nextIndex: Int)

/**
 * Recognizes one stage command beginning at `lines[index]`. Returns null if this
 * command does not start there. Each command implements this on its companion so the
 * registry can try them in turn.
 */
interface StageStepParser {
    fun tryParse(lines: List<String>, index: Int): ParsedStageStep?
}
