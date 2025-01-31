package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KLoggerFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

@Suppress("unused")
abstract class KFuzzPlugin : Plugin<Project> {
    val log = KLoggerFactory.getLogger(KFuzzPlugin::class)

    override fun apply(project: Project) {
        project.dependencies {
            add("testImplementation", "org.jetbrains:kotlinx.fuzz.api")
            add("testRuntimeOnly", "org.jetbrains:kotlinx.fuzz.gradle")
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

        val (defaultCP, defaultTCD) = project.defaultTestParameters()
        project.tasks.register<FuzzTask>("fuzz") {
            classpath = defaultCP
            testClassesDirs = defaultTCD
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

    /**
     * Finds the default values of classpath and testClassesDir properties of test tasks.
     * Required to fix https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_task_default_classpath for fuzz tests.
     *
     * @return the default values of classpath and testClassesDir properties of test tasks
     */
    private fun Project.defaultTestParameters(): Pair<FileCollection, FileCollection> =
        tasks.withType<Test>()
            .filterNot { it is FuzzTask }
            .map {
                log.debug { "Reusing the classpath of the task '${it.name}' for fuzz tests" }
                it.classpath to it.testClassesDirs
            }
            .singleOrNull()
            ?: run {
                log.warn { "'fuzz' task was not able to inherit the 'classpath' and 'testClassesDirs' properties, as it found conflicting configurations" }
                log.warn { "Please, specify them manually in your gradle config using the following syntax:" }
                log.warn {
                    """tasks.withType<FuzzTask>().configureEach {
                        classpath = TODO()
                        testClassesDirs = TODO()
                    }""".trimIndent()
                }
                project.files() to project.files()
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
