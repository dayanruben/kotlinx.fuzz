import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.io.File

fun findTargets(directory: File): Set<String> {
    val cb = ConfigurationBuilder().addUrls(directory.toURI().toURL())
        .setScanners(Scanners.MethodsAnnotated).forPackage("org.plan.research")
    val reflections = Reflections(cb)
    val annotation = "com.code_intelligence.jazzer.junit.FuzzTest"
    return reflections.store["MethodsAnnotated"]!![annotation]!!.map {
        it.trim().replaceAfter("(", "").replace("(", "")
    }.toSet()
}

abstract class PrintTargetNames : DefaultTask() {
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
