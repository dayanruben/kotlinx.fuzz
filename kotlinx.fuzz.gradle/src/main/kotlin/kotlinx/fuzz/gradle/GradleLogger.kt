package kotlinx.fuzz.gradle

import kotlinx.fuzz.KLogger
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GradleLogger(clazz: Class<*>) : KLogger(clazz) {
    private val log: Logger = Logging.getLogger(clazz)

    override fun debug(message: () -> String) {
        if (logLevel <= LogLevel.DEBUG) {
            log.lifecycle(message())
        }
    }

    override fun info(message: () -> String) {
        if (logLevel <= LogLevel.INFO) {
            log.lifecycle(message())
        }
    }

    override fun warn(message: () -> String) {
        if (logLevel <= LogLevel.WARN) {
            log.lifecycle(message())
        }
    }

    override fun error(message: () -> String) = log.error(message())

    companion object {
        const val LOG_LEVEL_PROPERTY = "kotlinx.fuzz.logging.level"
        private val logLevel: LogLevel = LogLevel.valueOf(System.getProperty(LOG_LEVEL_PROPERTY).uppercase())
    }
}
