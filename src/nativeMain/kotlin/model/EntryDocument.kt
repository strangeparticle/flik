package model

import command.ProcedureStep

/**
 * The parsed top-level entry document: its prerequisites and the ordered release
 * procedure. Sub-documents referenced by callouts are parsed when their step runs.
 */
data class EntryDocument(
    val title: String,
    val flikVersion: String?,
    val requiredEnvironmentVariables: List<String>,
    val requiredShellCommands: List<String>,
    val procedure: List<ProcedureStep>,
)
