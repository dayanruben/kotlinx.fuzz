package kotlinx.fuzz.gradle

import java.nio.file.Path
import kotlin.io.path.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class OverallStatsTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputFile
    abstract var outputFile: Path

    @TaskAction
    fun calculateOverallStats() {
        processCsvFiles(inputDir.get().asFile.toPath(), outputFile)
    }

    companion object {
        private fun processCsvFiles(inputDir: Path, outputFile: Path) {
            val csvFiles = inputDir.listDirectoryEntries("*.csv")
            val expectedHeader =
                csvFiles.firstOrNull()
                    ?.useLines { it.firstOrNull() }
                    ?.split(',')
                    ?.map { it.trim() }
                    ?: error("Can't compute overall stats: no CSV files found in $inputDir")

            val collectedRows = mutableListOf<List<String>>()

            for (file in csvFiles) {
                val rows = file.readLines()

                if (rows.isNotEmpty()) {
                    val currentHeader = rows.first().split(",").map { it.trim() }
                    check(currentHeader == expectedHeader) { "CSV file $file has different header. Expected '$expectedHeader' but got '$currentHeader'" }
                    val lastRow = rows.drop(1).lastOrNull()

                    lastRow?.let {
                        val newRow = listOf(file.nameWithoutExtension) + lastRow
                        collectedRows.add(newRow)
                    }
                    // TODO: else { log.warn }
                }
            }

            if (collectedRows.isNotEmpty()) {
                outputFile.writeText("target name,")
                outputFile.appendText(expectedHeader.joinToString(separator = ",", postfix = "\n"))
                outputFile.appendLines(collectedRows.map { it.joinToString(separator = ",") })
            } else {
                // TODO: log.error { "Can't compute overall stats: no CSV files found in $inputDir or no data rows found in any of them" }
            }
        }
    }
}
