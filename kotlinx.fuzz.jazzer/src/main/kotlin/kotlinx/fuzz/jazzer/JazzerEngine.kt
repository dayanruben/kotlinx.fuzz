package kotlinx.fuzz.jazzer

import java.io.InputStream
import java.io.ObjectInputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.debug
import kotlinx.fuzz.log.error
import java.lang.management.ManagementFactory

internal val Method.fullName: String
    get() = "${this.declaringClass.name}.${this.name}"

internal val KFuzzConfig.corpusDir: Path
    get() = workDir.resolve("corpus")

internal val KFuzzConfig.logsDir: Path
    get() = workDir.resolve("logs")

internal val KFuzzConfig.exceptionsDir: Path
    get() = workDir.resolve("exceptions")

@Suppress("unused")
class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val log = LoggerFacade.getLogger<JazzerEngine>()
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    companion object {
        private const val PORT = "5005"
        private const val DEBUG_SETUP_STRING =
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:$PORT"
    }

    override fun initialise() {
        config.corpusDir.createDirectories()
        config.logsDir.createDirectories()
        config.exceptionsDir.createDirectories()
    }

    private fun isDebugMode(): Boolean {
        return ManagementFactory.getRuntimeMXBean().inputArguments.any { it.contains("-agentlib:jdwp") }
    }

    override fun runTarget(instance: Any, method: Method): Throwable? {
        // spawn subprocess, redirect output to log and err files
        val classpath = System.getProperty("java.class.path")
        val javaCommand = System.getProperty("java.home") + "/bin/java"
        val properties = System.getProperties().map { (property, value) -> "-D$property=$value" }

        val debugOptions = if (isDebugMode()) {
            log.debug { "Connect Remote JVM Debugger (port $PORT) or attach to process with name \"kotlinx.fuzz.jazzer.JazzerLauncher\"" }
            listOf(DEBUG_SETUP_STRING)
        } else {
            emptyList()
        }

        val exitCode = ProcessBuilder(
            javaCommand,
            "-classpath", classpath,
            *debugOptions.toTypedArray(),
            *properties.toTypedArray(),
            JazzerLauncher::class.qualifiedName!!,
            method.declaringClass.name, method.name,
        ).executeAndSaveLogs(
            stdout = "${method.fullName}.log",
            stderr = "${method.fullName}.err",
        )

        return when (exitCode) {
            0 -> null
            else -> {
                val deserializedException = deserializeException(config.exceptionPath(method))
                deserializedException ?: run {
                    log.error { "Failed to deserialize exception for target '${method.fullName}'" }
                    Error("Failed to deserialize exception for target '${method.fullName}'")
                }
            }
        }
    }

    override fun finishExecution() {
        collectStatistics()
    }

    private fun collectStatistics() {
        val statsDir = config.workDir.resolve("stats").createDirectories()
        config.logsDir.listDirectoryEntries("*.err").forEach { file ->
            val csvText = jazzerLogToCsv(file, config.maxSingleTargetFuzzTime)
            statsDir.resolve("${file.nameWithoutExtension}.csv").writeText(csvText)
        }
    }

    private fun ProcessBuilder.executeAndSaveLogs(stdout: String, stderr: String): Int {
        val process = start()
        val stdoutStream = config.logsDir.resolve(stdout).outputStream()
        val stderrStream = config.logsDir.resolve(stderr).outputStream()
        val stdoutThread = logProcessStream(process.inputStream, stdoutStream) {
            if (jazzerConfig.enableLogging) {
                log.info(it)
            }
        }
        val stderrThread = logProcessStream(process.errorStream, stderrStream) {
            if (jazzerConfig.enableLogging) {
                log.info(it)
            }
        }
        val exitCode = process.waitFor()
        stdoutThread.join()
        stderrThread.join()
        return exitCode
    }

    private fun logProcessStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        log: (String?) -> Unit,
    ): Thread = thread(start = true) {
        inputStream.bufferedReader().use { reader ->
            outputStream.bufferedWriter().use { writer ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    writer.appendLine(line)
                    log(line)
                }
            }
        }
    }
}

internal fun KFuzzConfig.exceptionPath(method: Method): Path =
    exceptionsDir.resolve("${method.fullName}.exception")

internal fun Path.listCrashes(): List<Path> = listDirectoryEntries("{crash-*,timeout-*,slow-unit-*}")

/**
 * Reads a Throwable from the specified [path].
 */
private fun deserializeException(path: Path): Throwable? {
    path.inputStream().buffered().use { inputStream ->
        ObjectInputStream(inputStream).use { objectInputStream ->
            return objectInputStream.readObject() as? Throwable
        }
    }
}
