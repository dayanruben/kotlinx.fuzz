package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig.Companion.toPropertiesMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

abstract class KFuzzPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<FuzzTask>("fuzz") {
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz-test")
            }
        }
    }

    fun Project.fuzzConfig(block: KFuzzConfigBuilder.() -> Unit) {
        val properties = KFuzzConfigBuilder.build(block).toPropertiesMap()
        tasks.named<FuzzTask>("fuzz") {
            systemProperties(properties)
        }
    }
}

abstract class FuzzTask : Test() {
    @TaskAction
    fun action(): Unit = Unit
}
