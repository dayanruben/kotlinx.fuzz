package kotlinx.fuzz.gradle

import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.warn
import kotlinx.fuzz.regression.RegressionEngine
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

@Suppress("unused")
abstract class KFuzzPlugin : Plugin<Project> {
    val log = Logging.getLogger(KFuzzPlugin::class.java)!!

    override fun apply(project: Project) {
        val pluginVersion = "0.1.0"
        project.dependencies {
            add("testImplementation", "org.jetbrains:kotlinx.fuzz.api:$pluginVersion")
            add("testRuntimeOnly", "org.jetbrains:kotlinx.fuzz.gradle:$pluginVersion")
        }

        project.tasks.withType<Test>().configureEach {
            configureLogging()

            if (this !is FuzzTask && this !is RegressionTask) {
                useJUnitPlatform {
                    excludeEngines("kotlinx.fuzz")
                }
            }
        }

        val (defaultCP, defaultTCD) = project.defaultTestParameters()
        project.registerFuzzTask(defaultCP, defaultTCD)
        project.registerRegressionTask(defaultCP, defaultTCD)
    }

    private fun Project.registerFuzzTask(defaultCP: FileCollection, defaultTCD: FileCollection) {
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

    private fun Project.registerRegressionTask(defaultCP: FileCollection, defaultTCD: FileCollection) {
        project.tasks.register<RegressionTask>("regression") {
            classpath = defaultCP
            testClassesDirs = defaultTCD
            outputs.upToDateWhen { false }
            doFirst {
                systemProperties(fuzzConfig.toPropertiesMap() + mapOf(RegressionEngine.REGRESSION_PROPERTY to "true"))
            }
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz")
            }
        }
    }

    private fun Test.configureLogging() {
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
                log.debug("Reusing the classpath of the task '${it.name}' for fuzz tests")
                it.classpath to it.testClassesDirs
            }
            .singleOrNull()
            ?: run {
                log.warn("'fuzz' and 'regression' task was not able to inherit the 'classpath' and 'testClassesDirs' properties, as it found conflicting configurations")
                log.warn("Please, specify them manually in your gradle config using the following syntax:")
                log.warn("""
                    tasks.withType<FuzzTask>().configureEach {
                        classpath = TODO()
                        testClassesDirs = TODO()
                    }""".trimIndent(),
                )
                log.warn("""
                    tasks.withType<RegressionTask>().configureEach {
                        classpath = TODO()
                        testClassesDirs = TODO()
                    }""".trimIndent(),
                )
                project.files() to project.files()
            }
}

abstract class FuzzTask : Test() {
    private val log = LoggerFacade.getLogger<FuzzTask>()

    @Option(
        option = "fullClasspathReport",
        description = "Report on the whole classpath (not just the project classes).",
    )
    @get:Input
    var reportWithAllClasspath: Boolean = false

    @get:Internal
    internal lateinit var fuzzConfig: KFuzzConfig

    init {
        description = "Runs fuzzing"
        group = "verification"
    }

    @TaskAction
    fun action() {
        overallStats()
        if (fuzzConfig.dumpCoverage) {
            val workDir = fuzzConfig.workDir

            val coverageMerged = workDir.resolve("merged-coverage.exec")
            jacocoMerge(workDir.resolve("coverage"), coverageMerged)

            jacocoReport(coverageMerged, workDir)
        }
    }

    private fun overallStats() {
        val workDir = fuzzConfig.workDir
        overallStats(workDir.resolve("stats"), workDir.resolve("overall-stats.csv"))
    }

    private fun jacocoReport(execFile: Path, workDir: Path) {
        val extraDeps = getDependencies(fuzzConfig.jacocoReportIncludedDependencies)
        val mainSourceSet = project.extensions.getByType<SourceSetContainer>()["main"]
        val runtimeClasspath = project.configurations["runtimeClasspath"].files

        val projectClasspath = mainSourceSet.output.files
        val sourceDirectories = mainSourceSet.allSource.sourceDirectories.files

        val jacocoClassPath =
            projectClasspath + extraDeps + if (reportWithAllClasspath) runtimeClasspath else emptySet()

        jacocoReport(
            execFile = execFile,
            classPath = jacocoClassPath,
            sourceDirectories = sourceDirectories,
            reportDir = workDir.resolve("jacoco-report").createDirectories(),
            reports = fuzzConfig.jacocoReports,
        )
    }

    private fun getDependencies(dependencies: Set<String>): Set<File> {
        val configuration = project.configurations.findByName("runtimeClasspath") ?: run {
            log.warn { "No 'runtimeClasspath' configuration found, skipping jacoco report generation" }
            return emptySet()
        }

        val deps = configuration.resolvedConfiguration.resolvedArtifacts.associate {
            "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" to it.file.absoluteFile
        }
        return dependencies.mapNotNull { dependency ->
            deps[dependency] ?: run {
                log.warn { "Dependency '$dependency' not found in the classpath while generating jacoco report" }
                null
            }
        }.toSet()
    }
}

abstract class RegressionTask : Test() {
    @get:Internal
    internal lateinit var fuzzConfig: KFuzzConfig

    init {
        description = "Runs regression tests"
        group = "verification"
    }
}

@Suppress("unused")
fun Project.fuzzConfig(block: KFuzzConfigBuilder.() -> Unit) {
    val buildDir = layout.buildDirectory.get()
    val defaultWorkDir = buildDir.dir("fuzz").asFile.toPath()
    val config = KFuzzConfigBuilder.build {
        workDir = defaultWorkDir
        reproducerPath = defaultWorkDir.resolve("reproducers")
        block()
    }

    tasks.withType<FuzzTask>().forEach { task ->
        task.fuzzConfig = config
    }
    tasks.withType<RegressionTask>().forEach { task ->
        task.fuzzConfig = config
    }
}
