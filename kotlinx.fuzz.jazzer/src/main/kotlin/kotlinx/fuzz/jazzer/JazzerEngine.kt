package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.driver.Opt
import java.io.ObjectInputStream
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine
import kotlinx.fuzz.KLoggerFactory

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
    private val log = KLoggerFactory.getLogger(JazzerEngine::class)
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    init {
        val codeLocation = this::class.java.protectionDomain.codeSource.location
        val libsLocation = codeLocation.toURI()
            .toPath()
            .toFile()
            .parentFile
        System.load("$libsLocation/${System.mapLibraryName("casr_adapter")}")
    }

    private external fun parseAndClusterStackTraces(rawStacktraces: List<String>): List<Int>

    override fun initialise() {
        config.corpusDir.createDirectories()
        config.logsDir.createDirectories()
        config.exceptionsDir.createDirectories()
    }

    override fun runTarget(instance: Any, method: Method): Throwable? {
        // spawn subprocess, redirect output to log and err files
        val classpath = System.getProperty("java.class.path")
        val javaCommand = System.getProperty("java.home") + "/bin/java"
        val properties = System.getProperties().map { (property, value) -> "-D$property=$value" }

        val pb = ProcessBuilder(
            javaCommand,
            "-classpath", classpath,
            *properties.toTypedArray(),
            JazzerLauncher::class.qualifiedName!!,
            method.declaringClass.name, method.name,
        )
        pb.redirectError(config.logsDir.resolve("${method.fullName}.err").toFile())
        pb.redirectOutput(config.logsDir.resolve("${method.fullName}.log").toFile())

        val exitCode = pb.start().waitFor()
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
        clusterCrashes()
    }

    private fun collectStatistics() {
        val statsDir = config.workDir.resolve("stats").createDirectories()
        config.logsDir.listDirectoryEntries("*.err").forEach { file ->
            val csvText = jazzerLogToCsv(file, config.maxSingleTargetFuzzTime)
            statsDir.resolve("${file.nameWithoutExtension}.csv").writeText(csvText)
        }
    }

    private fun convertToJavaStyleStackTrace(kotlinStackTrace: String): String {
        val lines = kotlinStackTrace.lines()
        if (lines.isEmpty()) {
            return kotlinStackTrace
        }

        val firstLine = lines.first()
        val updatedFirstLine = if (firstLine.startsWith("Exception in thread \"main\"")) {
            firstLine
        } else {
            "Exception in thread \"main\" $firstLine"
        }

        return listOf(updatedFirstLine).plus(lines.drop(1)).joinToString("\n")
    }

    private fun clusterCrashes() {
        val directoryPath = Paths.get(Opt.reproducerPath.get()).absolute()
        val stacktraceFiles = directoryPath.listDirectoryEntries("stacktrace-*")

        val rawStackTraces = mutableListOf<String>()
        val fileMapping = mutableListOf<Pair<Path, Path>>()

        stacktraceFiles.forEach { file ->
            val crashFile = directoryPath.resolve("crash-${file.name.removePrefix("stacktrace-")}")
            val lines = convertToJavaStyleStackTrace(Files.readString(file))
            rawStackTraces.add(lines)
            fileMapping.add(file to crashFile)
        }

        val clusters = parseAndClusterStackTraces(rawStackTraces)
        val mapping = mutableMapOf<Int, Path>()

        clusters.forEachIndexed { index, cluster ->
            val (stacktraceSrc, crashSrc) = fileMapping[index]
            val isOld = mapping.containsKey(cluster)

            if (!mapping.containsKey(cluster)) {
                mapping[cluster] = directoryPath.resolve("cluster-${stacktraceSrc.readLines().first().trim()}")
            }

            val clusterDir = directoryPath.resolve(mapping[cluster]!!)
            if (!clusterDir.exists()) {
                clusterDir.createDirectory()
            }

            stacktraceSrc.copyTo(clusterDir.resolve(stacktraceSrc.fileName), overwrite = true)
            if (isOld) {
                stacktraceSrc.deleteExisting()
            }

            crashSrc.copyTo(clusterDir.resolve(crashSrc.fileName), overwrite = true)
            if (isOld) {
                crashSrc.deleteExisting()
            }
        }
    }
}

internal fun KFuzzConfig.exceptionPath(method: Method): Path =
    exceptionsDir.resolve("${method.fullName}.exception")

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
