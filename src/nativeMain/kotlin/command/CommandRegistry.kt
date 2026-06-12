package command

/**
 * The command registry. Adding a new command means adding one file in this package
 * and registering its parser here — the parser loop and the executor need no changes.
 */

val stageStepParsers: List<StageStepParser> = listOf(
    CopyFileStep,
    RunCommandStep,
    ExpectFileExistsStep,
    EditInPlaceStep,
    RunChecksStep,
)

val procedureStepParsers: List<ProcedureStepParser> = listOf(
    BackUpStep,
    RestoreStep,
    CalloutStep,
)
