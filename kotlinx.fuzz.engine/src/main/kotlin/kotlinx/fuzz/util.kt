package kotlinx.fuzz

import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.fuzz.config.KFuzzConfig

fun String?.toBooleanOrTrue(): Boolean = this?.toBoolean() != false
fun String?.toBooleanOrFalse(): Boolean = this?.toBoolean() == true

fun KFuzzConfig.reproducerPathOf(method: Method): Path =
    Path(global.reproducerDir.absolutePathString(), method.declaringClass.name, method.name).absolute()

fun Path.listCrashes(): List<Path> = if (this.exists()) listDirectoryEntries("{crash-*,timeout-*,slow-unit-*}") else emptyList()

internal fun String.asList(separator: String = ",") =
    this.split(separator)
        .map(String::trim)
        .filter(String::isNotEmpty)
