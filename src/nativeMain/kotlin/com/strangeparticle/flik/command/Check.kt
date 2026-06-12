package com.strangeparticle.flik.command

/** One verification check: a command and an optional substring its output must contain. */
data class Check(val description: String, val command: String, val expectContains: String?)
