package kotlinx.fuzz.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register

abstract class KFuzzPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val fuzzTask = project.tasks.register<FuzzTask>("fuzz")
    }
}

abstract class FuzzTask : Test() {
    @TaskAction
    fun action() {
        println("Invoking FuzzTask")
    }
}