package model

/** A parsed stage sub-document referenced by a callout. */
data class SubDocument(
    val title: String,
    val flikVersion: String?,
    val commands: List<StageCommand>,
)
