import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.Property
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.dependencies

abstract class DownloadDependency : DefaultTask() {

    @get:Input
    abstract val targetDirectory: Property<String>

    @get:Input
    abstract val dependencyName: Property<String>

    @TaskAction
    fun download() {
        val depName = dependencyName.get()
        val parts = depName.split(':')
        if (parts.size != 3) {
            throw GradleException("Dependency name must be in the format 'group:name:version'")
        }
        val (group, name, version) = parts

        val dependencyDownload = project.configurations.create("dependencyDownload")
        project.dependencies {
            add("dependencyDownload", "$group:$name:$version")
        }

        val dependencyFiles = dependencyDownload.resolve()
        val targetDir = project.file(targetDirectory.get())
        targetDir.mkdirs()
        println("dependencyFiles: $dependencyFiles")
        dependencyFiles.forEach { file ->
            project.copy {
                from(file)
                into(targetDir)
            }
        }

        println("Downloaded $depName to $targetDir")
    }
}