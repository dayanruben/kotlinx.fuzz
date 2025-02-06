package kotlinx.fuzz.regression

import kotlinx.fuzz.KFuzzerImpl
import kotlinx.fuzz.log.*
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes

object RegressionEngine {
    const val REGRESSION_PROPERTY = "kotlinx.fuzz.regression"

    private val log = LoggerFacade.getLogger<RegressionEngine>()

    fun runOneCrash(instance: Any, method: Method, crash: Path): Throwable? {
        log.debug { "Executing ${crash.name} for method ${method.name}" }
        try {
            method.invoke(instance, KFuzzerImpl(crash.readBytes()))
        } catch (e: Throwable) {
            return e
        }
        return null
    }

}

fun Path.listCrashes(): List<Path> = listDirectoryEntries("crash-*")
