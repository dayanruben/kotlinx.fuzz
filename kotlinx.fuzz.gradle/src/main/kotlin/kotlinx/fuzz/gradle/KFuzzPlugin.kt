package kotlinx.fuzz.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

abstract class KFuzzPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies.add(
            "testImplementation",
            "kotlinx.fuzz:kotlinx.fuzz.api",
        )
        project.tasks.register<FuzzTask>("fuzz") {
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz-test")
            }
        }
        project.dependencies.add(
            "testImplementation",
            "kotlinx.fuzz:kotlinx.fuzz.gradle",
        )
        project.dependencies.add(
            "testRuntimeOnly",
            "kotlinx.fuzz:kotlinx.fuzz.jazzer",
        )

        project.extensions.create<KonfTest>("konfTest")
    }
}

abstract class KonfTest() {
    abstract var a: Int
    abstract var b: String
}


abstract class FuzzTask : Test() {
    @TaskAction
    fun action(): Unit = Unit
}
