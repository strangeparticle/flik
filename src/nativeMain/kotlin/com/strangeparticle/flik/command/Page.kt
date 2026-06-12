package com.strangeparticle.flik.command

/**
 * A parsed Flik page: prerequisites declared at the top, then a body of elements.
 * Every page has the same shape — the page where execution starts (the "root") is
 * not special; it is simply the one passed to `flik run`.
 */
data class Page(
    val title: String,
    val flikVersion: String?,
    val requiredEnvironmentVariables: List<String>,
    val requiredShellCommands: List<String>,
    val elements: List<PageElement>,
)
