package kotlinx.fuzz.task.target

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CheckTargetsExist : TargetTask() {

    @get:InputDirectory
    abstract val classpathDir: DirectoryProperty

    @get:InputFile
    @get:Option(option = "targetsList", description = "Path to file with list of target names")
    abstract val targetsList: RegularFileProperty

    @TaskAction
    fun action() {
        val targets = findTargets(classpathDir.get().asFile)
        targetsList.get().asFile.useLines { requestedTargets ->
            val missingTargets =
                requestedTargets.filterNot { it.isBlank() }.filter { it !in targets }.toList()
            if (missingTargets.isNotEmpty()) {
                throw Exception("Targets not found:\n${missingTargets.joinToString("\n")}")
            }
        }
    }
}
