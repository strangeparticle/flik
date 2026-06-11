package parse

import model.Check
import model.StageCommand
import model.SubDocument

private val COPY_REGEX = Regex("\\* Copy file from: `(.+?)` to: `(.+?)`")
private val IN_FILE_REGEX = Regex("In file: `(.+?)`")
private val EXPECT_REGEX = Regex("Expect file to exist: `(.+?)`")
private val CHECK_COMMAND_REGEX = Regex("run `([^`]+)`")
private val CHECK_CONTAINS_REGEX = Regex("contain `([^`]+)`")

/** Parses a stage sub-document into its ordered [StageCommand]s. */
fun parseSubDocument(source: String): SubDocument {
    val lines = source.lines()

    val title = lines.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")?.trim()
        ?: throw FlikParseException("sub-document has no '# Title' heading")
    val flikVersion = lines
        .firstOrNull { it.trim().startsWith("flik version:", ignoreCase = true) }
        ?.substringAfter(":")?.trim()

    val commands = mutableListOf<StageCommand>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trim()
        val copy = COPY_REGEX.matchEntire(line)
        val inFile = IN_FILE_REGEX.matchEntire(line)
        val expect = EXPECT_REGEX.matchEntire(line)
        when {
            copy != null -> {
                commands.add(StageCommand.CopyFile(copy.groupValues[1], copy.groupValues[2]))
                index++
            }
            expect != null -> {
                commands.add(StageCommand.ExpectFileExists(expect.groupValues[1]))
                index++
            }
            line == "Run command:" -> {
                val (block, next) = readFencedBlock(lines, index + 1)
                commands.add(StageCommand.RunCommand(block.trim()))
                index = next
            }
            inFile != null -> {
                index = parseEditInPlace(lines, index, inFile.groupValues[1], commands)
            }
            line == "Run these checks in parallel:" -> {
                val (checks, next) = readChecks(lines, index + 1)
                commands.add(StageCommand.ParallelChecks(checks))
                index = next
            }
            else -> index++
        }
    }

    return SubDocument(title, flikVersion, commands)
}

private fun parseEditInPlace(
    lines: List<String>,
    inFileIndex: Int,
    file: String,
    commands: MutableList<StageCommand>,
): Int {
    val findLabel = indexOfTrimmed(lines, inFileIndex + 1, "Find this section:")
    if (findLabel == -1) throw FlikParseException("expected 'Find this section:' after In file: `$file`")
    val (findBlock, afterFind) = readFencedBlock(lines, findLabel + 1)

    val replaceLabel = indexOfTrimmed(lines, afterFind, "Replace it with:")
    if (replaceLabel == -1) throw FlikParseException("expected 'Replace it with:' for In file: `$file`")
    val (replaceBlock, afterReplace) = readFencedBlock(lines, replaceLabel + 1)

    commands.add(StageCommand.EditInPlace(file, findBlock, replaceBlock))
    return afterReplace
}

private fun indexOfTrimmed(lines: List<String>, from: Int, target: String): Int {
    for (j in from until lines.size) {
        if (lines[j].trim() == target) return j
    }
    return -1
}

/** Reads the next fenced ``` block starting at-or-after [from]. Returns its body and the index after the closing fence. */
private fun readFencedBlock(lines: List<String>, from: Int): Pair<String, Int> {
    var j = from
    while (j < lines.size && lines[j].trim().isEmpty()) j++
    if (j >= lines.size || !lines[j].trim().startsWith("```")) {
        throw FlikParseException("expected a fenced code block near line ${j + 1}")
    }
    val body = mutableListOf<String>()
    j++
    while (j < lines.size && !lines[j].trim().startsWith("```")) {
        body.add(lines[j])
        j++
    }
    if (j >= lines.size) throw FlikParseException("unterminated fenced code block")
    return body.joinToString("\n") to (j + 1)
}

private fun readChecks(lines: List<String>, from: Int): Pair<List<Check>, Int> {
    val checks = mutableListOf<Check>()
    var j = from
    while (j < lines.size) {
        val trimmed = lines[j].trim()
        if (trimmed.startsWith("* ")) {
            val command = CHECK_COMMAND_REGEX.find(trimmed)?.groupValues?.get(1)
            if (command != null) {
                val contains = CHECK_CONTAINS_REGEX.find(trimmed)?.groupValues?.get(1)
                checks.add(Check(trimmed.removePrefix("* ").trim(), command, contains))
            }
            j++
        } else if (trimmed.isEmpty() && checks.isEmpty()) {
            j++
        } else {
            break
        }
    }
    return checks to j
}
