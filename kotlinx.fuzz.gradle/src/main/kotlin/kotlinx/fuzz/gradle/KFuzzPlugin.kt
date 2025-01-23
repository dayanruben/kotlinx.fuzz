package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

@Suppress("unused")
abstract class KFuzzPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies {
            add("testImplementation", "kotlinx.fuzz:kotlinx.fuzz.api")
            add("testRuntimeOnly", "kotlinx.fuzz:kotlinx.fuzz.gradle")
        }

        val extension = project.extensions.create<KFuzzExtension>("kfuzz")

        project.tasks.register<FuzzTask>("fuzz") {
            val fuzzConfig = extension.fuzzConfig
            doFirst {
                systemProperties(fuzzConfig.toPropertiesMap())
            }
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz")
            }
        }

        project.tasks.named<Test>("test") {
            useJUnitPlatform {
                excludeEngines("kotlinx.fuzz")
            }
        }
    }
}

open class KFuzzExtension internal constructor() {
    internal lateinit var fuzzConfig: KFuzzConfig
        private set

    @Suppress("unused")
    fun config(block: KFuzzConfigBuilder.() -> Unit) {
        fuzzConfig = KFuzzConfigBuilder.build(block)
    }
}

abstract class FuzzTask : Test() {
    @TaskAction
    fun action(): Unit = Unit
}
