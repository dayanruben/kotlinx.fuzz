package kotlinx.fuzz

import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.reproducer.CrashReproducerGenerator

interface KFuzzEngine {
    /**
     * Config of the fuzzer engine. Includes both global and engine-specific configs
     */
    val config: KFuzzConfig

    /**
     * Initialises engine. Should be called only once for every KFuzzEngine instance
     */
    fun initialise()

    /**
     * Runs engine on the specified target. Crashes should be saved in "<reproducerPath>/<class name>/<method name>".
     * Each crash should be represented as file with name that starts with "crash-", "timeout-" or "slow-unit- and contain byte array
     * that can be passed as input to KFuzzerImpl to reproduce the crash
     *
     * @param instance --- instance of a class that contains method under fuzzing (harness)
     * @param method --- harness itself
     * @return nullable throwable. Null iff harness ran without failures, cause (look at throwable field in
     * org.junit.platform.engine.TestExecutionResult) otherwise
     */
    fun runTarget(
        instance: Any,
        method: Method,
    ): Throwable?

    /**
     * Runs any needed postwork, like rearranging crash files
     * @param reproducerGenerator --- generates reproducer tests from found crashes
     */
    fun finishExecution(
        reproducerGenerator: CrashReproducerGenerator? = null,
    )
}


internal fun Path.listStackTraces(): List<Path> = listDirectoryEntries("stacktrace-*")

fun KFuzzEngine.clusterCrashes() = clusterCrashesAndGenerateReproducers()

fun KFuzzEngine.clusterCrashesAndGenerateReproducers(
    reproducer: (String, String) -> CrashReproducerGenerator? = { _, _ -> null },
) {
    val filesForDeletion = mutableListOf<Path>()
    Files.walk(config.global.reproducerDir)
        .filter { it.isDirectory() && it.name.startsWith("cluster-") }
        .map { it to it.listStackTraces() }
        .flatMap { (dir, files) -> files.stream().map { dir to it } }
        .forEach { (clusterDir, stacktraceFile) ->
            val methodName = clusterDir.parent.fileName.toString()
            val className = clusterDir.parent.parent.fileName.toString()

            val crashFileName = "crash-${stacktraceFile.name.removePrefix("stacktrace-")}"
            val crashFile = clusterDir.parent.resolve(crashFileName)
            val targetCrashFile = clusterDir.resolve(crashFileName)
            val reproducerFileName = "reproducer-${stacktraceFile.name.removePrefix("stacktrace-")}.kt"
            val reproducerFile = clusterDir.parent.resolve(reproducerFileName)
            val targetReproducerFile = clusterDir.resolve(reproducerFileName)

            if (targetCrashFile.exists() || !crashFile.exists()) {
                return@forEach
            }

            crashFile.copyTo(targetCrashFile, overwrite = true)
            val reproducerWriter = reproducer(className, methodName)
            if (!reproducerFile.exists() && reproducerWriter != null) {
                reproducerWriter.generateToPath(crashFile.readBytes(), reproducerFile)
            }
            if (!clusterDir.name.endsWith(crashFileName.removePrefix("crash-"))) {
                filesForDeletion.add(crashFile)
                if (reproducerFile.exists()) {
                    reproducerFile.copyTo(targetReproducerFile, overwrite = true)
                    filesForDeletion.add(reproducerFile)
                }
            } else if (reproducerFile.exists()) {
                reproducerFile.copyTo(targetReproducerFile, overwrite = true)
            }
        }
    filesForDeletion.forEach { it.deleteIfExists() }
}
