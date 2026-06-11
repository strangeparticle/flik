package validate

import model.EntryDocument
import model.ProcedureStep
import parse.calloutFileName

/**
 * Returns human-readable problems with an entry document, or an empty list if it is
 * valid. Filesystem access is injected via [calloutFileExists] (given a normalized
 * filename) so this is pure and unit-testable.
 */
fun findEntryDocumentProblems(
    document: EntryDocument,
    calloutFileExists: (fileName: String) -> Boolean,
): List<String> {
    val problems = mutableListOf<String>()

    if (document.flikVersion == null) {
        problems.add("missing 'flik version:' in the prerequisites section")
    }

    for (step in document.procedure) {
        if (step is ProcedureStep.Callout) {
            val fileName = calloutFileName(step.name)
            if (!calloutFileExists(fileName)) {
                problems.add("callout \"${step.name}\" resolves to $fileName, which was not found")
            }
        }
    }

    return problems
}
