package command

/**
 * One element of a page body. A page is a flat sequence of elements executed in
 * document order. There are exactly two kinds: a [Command] (a built-in) and a
 * [PageInvocation] (a call to another page).
 */
interface PageElement {
    fun execute(context: ExecutionContext)
}

/** Marker for built-in commands, as distinct from page invocations. */
interface Command : PageElement

/** A successful element parse: the parsed element and the line index just past it. */
data class ParsedElement(val element: PageElement, val nextIndex: Int)

/**
 * Recognizes one page element beginning at `lines[index]`, or returns null. Each
 * command and the page-invocation implement this on their companion, so the registry
 * can try them in document order.
 */
interface PageElementParser {
    fun tryParse(lines: List<String>, index: Int): ParsedElement?
}
