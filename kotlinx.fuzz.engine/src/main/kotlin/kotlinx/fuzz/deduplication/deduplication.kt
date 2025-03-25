package kotlinx.fuzz.deduplication

import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.set
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlinx.fuzz.KFuzzEngine
import kotlinx.fuzz.flatten
import kotlinx.fuzz.listClusters
import kotlinx.fuzz.listStackTraces
import kotlinx.fuzz.reproducer.CrashReproducerGenerator
import org.jetbrains.casr.adapter.CasrAdapter

fun KFuzzEngine.initializeClusters() {
    config.global.reproducerDir.listDirectoryEntries()
        .filter { it.isDirectory() }
        .forEach { classDir ->
            classDir.listDirectoryEntries()
                .filter { it.isDirectory() }
                .forEach { methodDir ->
                    flatten(methodDir)
                    clusterCrashes(methodDir)
                }
        }
    cleanupCrashesAndGenerateReproducers { _, _ -> null }
}

fun KFuzzEngine.cleanupCrashesAndGenerateReproducers(
    reproducer: (String, String) -> CrashReproducerGenerator?,
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


/**
 * Clusters all crashes located in the directory and returns the number of unique crashes
 *
 * @param directoryPath --- path to the directory with crashes
 * @return number of unique crashes
 */
fun clusterCrashes(directoryPath: Path): Int {
    val stacktraceFiles = directoryPath.listStackTraces()

    val rawStackTraces = mutableListOf<String>()

    stacktraceFiles.forEach { file ->
        val lines = convertToJavaStyleStackTrace(file.readText())
        rawStackTraces.add(lines)
    }

    val clusters = CasrAdapter.parseAndClusterStackTraces(rawStackTraces)
    val mapping = initClustersMapping(directoryPath, stacktraceFiles, clusters)

    clusters.forEachIndexed { index, cluster ->
        val stacktraceSrc = stacktraceFiles[index]

        if (!mapping.containsKey(cluster)) {
            mapping[cluster] = directoryPath.resolve("cluster-${stacktraceSrc.name.removePrefix("stacktrace-")}")
        }

        val clusterDir = directoryPath.resolve(mapping[cluster]!!)
        if (!clusterDir.exists()) {
            clusterDir.createDirectory()
        }

        stacktraceSrc.copyTo(clusterDir.resolve(stacktraceSrc.name), overwrite = true)
        if (mapping[cluster]!!.name.removePrefix("cluster-") != stacktraceSrc.name.removePrefix("stacktrace-")) {
            stacktraceSrc.deleteExisting()
        }
    }

    return clusters.maxOrNull() ?: 0
}

internal fun initClustersMapping(
    directoryPath: Path,
    stacktraceFiles: List<Path>,
    clusters: List<Int>,
): MutableMap<Int, Path> {
    val mapping = mutableMapOf<Int, Path>()
    directoryPath.listClusters().map { it.name.removePrefix("cluster-") }.forEach { hash ->
        val clusterId = clusters[stacktraceFiles.indexOfFirst { it.name.endsWith(hash) }]
        mapping[clusterId] = directoryPath.resolve("cluster-$hash")
    }
    return mapping
}

internal fun convertToJavaStyleStackTrace(kotlinStackTrace: String): String {
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
