package com.strangeparticle.flik.program

/** A structural problem found while linking, tagged with the page (file) it came from. */
data class Diagnostic(val page: String, val message: String)
