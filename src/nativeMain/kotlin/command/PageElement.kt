package command

/**
 * One element of a page body. A page is a flat sequence of elements executed in
 * document order. There are exactly two kinds: a [Command] (a built-in) and a
 * [PageInvocation] (a call to another page).
 */
interface PageElement {
    fun execute(context: ExecutionContext)
}
