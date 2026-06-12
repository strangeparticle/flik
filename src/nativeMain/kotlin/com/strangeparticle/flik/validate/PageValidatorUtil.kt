package com.strangeparticle.flik.validate

import com.strangeparticle.flik.command.Page
import com.strangeparticle.flik.command.PageInvocation
import com.strangeparticle.flik.parse.calloutFileName

/**
 * Returns human-readable problems with a page, or an empty list if it is valid.
 * Filesystem access is injected via [pageFileExists] (given a normalized filename) so
 * this is pure and unit-testable. Checks the page's direct invocations only.
 */
fun findPageProblems(
    page: Page,
    pageFileExists: (fileName: String) -> Boolean,
): List<String> {
    val problems = mutableListOf<String>()

    if (page.flikVersion == null) {
        problems.add("missing 'flik version:' in the prerequisites section")
    }

    for (element in page.elements) {
        if (element is PageInvocation) {
            val fileName = calloutFileName(element.name)
            if (!pageFileExists(fileName)) {
                problems.add("page invocation \"${element.name}\" resolves to $fileName, which was not found")
            }
        }
    }

    return problems
}
