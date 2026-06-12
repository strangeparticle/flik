package command

/** A successful element parse: the parsed element and the line index just past it. */
data class ParsedElement(val element: PageElement, val nextIndex: Int)
