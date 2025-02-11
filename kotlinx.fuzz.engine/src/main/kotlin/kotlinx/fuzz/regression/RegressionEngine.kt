package kotlinx.fuzz.regression

import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.fuzz.KFuzzerImpl

object RegressionEngine {
    fun runOneCrash(instance: Any, method: Method, crash: Path): Throwable? = try {
        method.invoke(instance, KFuzzerImpl(crash.readBytes()))
        null
    } catch (e: Throwable) {
        e
    }
}

fun Path.listCrashes(): List<Path> = if (this.exists()) listDirectoryEntries("crash-*") else emptyList()
