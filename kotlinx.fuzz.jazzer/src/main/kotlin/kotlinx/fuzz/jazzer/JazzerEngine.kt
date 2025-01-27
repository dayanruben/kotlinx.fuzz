package kotlinx.fuzz.jazzer

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.*

@Suppress("unused", "SpellCheckingInspection")
class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    override fun initialise() {
        config.corpusDir.createDirectories()
        config.logsDir.createDirectories()
    }


    @OptIn(ExperimentalPathApi::class)
    override fun runTarget(instance: Any, method: Method): Throwable? {
        // spawn subprocess, redirect output to log and err files

        val classpath = System.getProperty("java.class.path")
        val mainClass = "kotlinx.fuzz.jazzer.Launcher"
        val javaCommand = System.getProperty("java.home") + "/bin/java"

        // to pass `config` to new process
        val properties =
            config.toPropertiesMap().map { (key, value) -> "-D$key=$value" }.toTypedArray()

        val pb = ProcessBuilder(
            javaCommand,
            "-classpath", classpath,
            *properties,
            mainClass,
            method.declaringClass.name, method.name,
        )
        pb.redirectError(config.logsDir.resolve("${method.fullName}.err").toFile())
        pb.redirectOutput(config.logsDir.resolve("${method.fullName}.log").toFile())

        val res = pb.start().waitFor()
        if (res == 0) {
            return null
        }
        // TODO: read real exception
        return Throwable("Jazzer subprocess returned with code $res")
    }

    override fun finishExecution() {
        val statsDir = config.workDir.resolve("stats").createDirectories()
        config.logsDir.listDirectoryEntries("*.err").forEach { file ->
            val csvText = jazzerLogToCsv(file, config.maxSingleTargetFuzzTime)
            statsDir.resolve("${file.nameWithoutExtension}.csv").writeText(csvText)
        }
    }
}

internal val Method.fullName: String
    get() = "${this.declaringClass.name}.${this.name}"

internal val KFuzzConfig.corpusDir: Path
    get() = workDir.resolve("corpus")

internal val KFuzzConfig.logsDir: Path
    get() = workDir.resolve("logs")
