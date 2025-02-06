package kotlinx.fuzz.regression

import kotlinx.fuzz.KFuzzerImpl
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.*

object RegressionEngine {
    const val REGRESSION_PROPERTY = "kotlinx.fuzz.regression"

    fun runOneCrash(instance: Any, method: Method, crash: Path): Throwable? {
        try {
            method.invoke(instance, KFuzzerImpl(crash.readBytes()))
        } catch (e: Throwable) {
            return e
        }
        return null
    }
}

fun Path.listCrashes(): List<Path> = if (this.exists()) listDirectoryEntries("crash-*") else emptyList()
