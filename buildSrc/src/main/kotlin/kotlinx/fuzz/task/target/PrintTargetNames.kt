package kotlinx.fuzz.task.target

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class PrintTargetNames : TargetTask() {
    @get:InputDirectory
    abstract val classpathDir: DirectoryProperty

    @get:OutputFile
    @get:Option(option = "outputFile", description = "File to write target names")
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun action() {
        val targets = findTargets(classpathDir.get().asFile)
        val s = targets.map {
            val s = it.split('.')
            s.subList(0, s.size - 1).joinToString(".") to s.last()
        }.groupBy({ it.first },
            { it.second }).entries.joinToString(separator = "\n\n") { (key, value) ->
            value.joinToString(separator = "\n") { "$key.$it" }
        }
        println(s)
        outputFile.get().asFile.writeText(s)
    }
}
