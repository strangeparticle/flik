package com.strangeparticle.flik.command

/**
 * One element of a page body. A page is a flat sequence of elements executed in
 * document order. There are exactly two kinds: a [Command] (a built-in) and a
 * [PageInvocation] (a call to another page).
 */
interface PageElement {
    fun execute(context: ExecutionContext)

    /**
     * The page invocations this element may run — directly, or inside its branches.
     * Linking walks this so a page reached only through a conditional/switch branch is
     * still resolved, cycle-checked, and contributes its prerequisites. Most elements
     * reference no pages; [PageInvocation] is itself one; branching commands flat-map
     * over their branches.
     */
    fun referencedPageInvocations(): List<PageInvocation> = emptyList()
}
