package com.strangeparticle.flik.command

/** Raised when a step fails; aborts the run (fail-on-error). */
class FlikExecutionException(message: String) : Exception(message)
