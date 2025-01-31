package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KLoggerFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.cc.base.logger
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

        // Fix https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_task_default_classpath for fuzz tests
        val (cp, tcd) = project.tasks.withType<Test>()
            .firstOrNull { it !is FuzzTask }
            ?.let { it.classpath to it.testClassesDirs }
            ?: run {
                logger.warn("There were no test tasks found, so 'fuzz' task did not inherit default classpath and testClassesDirs")
                logger.warn("Please, specify them manually in your gradle config using the following syntax:")
                logger.warn("""
                    tasks.withType<FuzzTask>().configureEach {
                        classpath = TODO()
                        testClassesDirs = TODO()
                    }
                """.trimIndent())
                project.files() to project.files()
            }

        project.tasks.register<FuzzTask>("fuzz") {
            classpath = cp
            testClassesDirs = tcd
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
        val userLoggingLevel = System.getProperty(GradleLogger.LOG_LEVEL_PROPERTY)
        val projectLogLevel = project.gradle.startParameter.logLevel

        systemProperties[GradleLogger.LOG_LEVEL_PROPERTY] = when {
            userLoggingLevel?.uppercase() in LogLevel.values().map { it.name } -> userLoggingLevel
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
