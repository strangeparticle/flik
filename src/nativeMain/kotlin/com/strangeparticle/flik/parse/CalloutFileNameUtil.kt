package com.strangeparticle.flik.parse

/**
 * Resolves a bare callout name (e.g. "Re-apply release-only repo changes") to the
 * sibling document filename Flik looks for. Normalization, per the language spec:
 *   1. lowercase
 *   2. replace every run of non-[a-z0-9] characters with a single '-'
 *   3. trim leading/trailing '-'
 *   4. append ".flik.md"
 */
fun calloutFileName(calloutName: String): String {
    val slug = calloutName
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "$slug.flik.md"
}
