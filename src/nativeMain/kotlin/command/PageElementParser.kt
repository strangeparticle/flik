package command

/**
 * Recognizes one page element beginning at `lines[index]`, or returns null. Each
 * command and the page-invocation implement this on their companion, so the registry
 * can try them in document order.
 */
interface PageElementParser {
    fun tryParse(lines: List<String>, index: Int): ParsedElement?
}
