package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.LoggerFacade
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
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

        project.tasks.withType<Test>().configureEach {
            configureLogging(project)

            if (this is FuzzTask) {
                return@configureEach
            }

            useJUnitPlatform {
                excludeEngines("kotlinx.fuzz")
            }
        }

        project.tasks.register<FuzzTask>("fuzz") {
            outputs.upToDateWhen { false }  // so the task will run on every invocation
            doFirst {
                systemProperties(fuzzConfig.toPropertiesMap())
            }
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz")
            }
        }
    }

    private fun Test.configureLogging(project: Project) {
        val userLoggingLevel = System.getProperty(LoggerFacade.LOG_LEVEL_PROPERTY)
        val projectLogLevel = project.gradle.startParameter.logLevel

        systemProperties[LoggerFacade.LOG_LEVEL_PROPERTY] = when {
            userLoggingLevel in LogLevel.values().map { it.name } -> userLoggingLevel
            projectLogLevel == LogLevel.LIFECYCLE -> LogLevel.WARN.name
            else -> projectLogLevel.name
        }

        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }
    }
}

abstract class FuzzTask : Test() {
    @get:Internal
    internal lateinit var fuzzConfig: KFuzzConfig

    @TaskAction
    fun action(): Unit = Unit
}

@Suppress("unused")
fun Project.fuzzConfig(block: KFuzzConfigBuilder.() -> Unit) {
    val config = KFuzzConfigBuilder.build(block)
    tasks.withType<FuzzTask>().forEach { task ->
        task.fuzzConfig = config
    }
}
