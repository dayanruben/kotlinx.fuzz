package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KLoggerFactory
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

    /**
     * Configures logging as in kotlinx.fuzz.gradle/build.gradle.kts and in buildSrc/src/main/kotlin/kotlinx.fuzz.src-module.gradle.kts
     * If changed, consider changing there as well
     */
    private fun Test.configureLogging(project: Project) {
        val userLoggingLevel = System.getProperty(GradleLogger.LOG_LEVEL_PROPERTY, "null")
        val projectLogLevel = project.gradle.startParameter.logLevel

        systemProperties[GradleLogger.LOG_LEVEL_PROPERTY] = when {
            userLoggingLevel.uppercase() in LogLevel.values().map { it.name } -> userLoggingLevel
            projectLogLevel == LogLevel.LIFECYCLE -> LogLevel.WARN.name
            else -> projectLogLevel.name
        }

        systemProperties[KLoggerFactory.LOGGER_IMPLEMENTATION_PROPERTY] = GradleLogger::class.qualifiedName

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
    fun action() {
        overallStats()
    }

    private fun overallStats() {
        val workDir = fuzzConfig.workDir
        overallStats(workDir.resolve("stats"), workDir.resolve("overall-stats.csv"))
    }
}

@Suppress("unused")
fun Project.fuzzConfig(block: KFuzzConfigBuilder.() -> Unit) {
    val buildDir = layout.buildDirectory.get()
    val defaultWorkDir = buildDir.dir("fuzz").asFile.toPath()
    val config = KFuzzConfigBuilder.build {
        workDir = defaultWorkDir
        block()
    }

    tasks.withType<FuzzTask>().forEach { task ->
        task.fuzzConfig = config
    }
}
