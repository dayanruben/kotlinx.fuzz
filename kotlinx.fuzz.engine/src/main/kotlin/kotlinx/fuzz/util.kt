package kotlinx.fuzz

import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.fuzz.config.KFuzzConfig

fun Path.listStackTraces(): List<Path> = listDirectoryEntries("stacktrace-*")

fun Path.listClusters(): List<Path> = listDirectoryEntries("cluster-*")

fun String?.toBooleanOrTrue(): Boolean = this?.toBoolean() != false
fun String?.toBooleanOrFalse(): Boolean = this?.toBoolean() == true

fun KFuzzConfig.reproducerPathOf(method: Method): Path =
    Path(global.reproducerDir.absolutePathString(), method.declaringClass.name, method.name).absolute()

fun Path.listCrashes(): List<Path> = if (this.exists()) listDirectoryEntries("{crash-*,timeout-*,slow-unit-*}") else emptyList()

internal fun String.asList(separator: String = ",") =
    this.split(separator)
        .map(String::trim)
        .filter(String::isNotEmpty)

/**
 * Moves all files from the nested directories to the top one and deletes all nested dirs
 *
 * @param dir --- directory to flatten
 */
@OptIn(ExperimentalPathApi::class)
internal fun flatten(dir: Path) {
    Files.walk(dir).filter { it.isRegularFile() }.forEach {
        val targetFile = dir.resolve(it.name)
        if (targetFile.exists()) {
            return@forEach
        }
        it.copyTo(targetFile)
    }
    dir.listDirectoryEntries().filter { it.isDirectory() }.forEach { it.deleteRecursively() }
}
