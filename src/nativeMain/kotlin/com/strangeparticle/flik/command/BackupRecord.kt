package com.strangeparticle.flik.command

/** A backup taken during the run: which page created it, its filename, and full path. */
data class BackupRecord(val pageId: String, val fileName: String, val filePath: String)
