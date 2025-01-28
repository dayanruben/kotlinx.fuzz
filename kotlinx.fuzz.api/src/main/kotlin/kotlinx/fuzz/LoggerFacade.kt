package kotlinx.fuzz

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class LoggerFacade(clazz: Class<*>) {
    private val log: Logger = Logging.getLogger(clazz)

    fun debug(message: () -> String) {
        if (logLevel <= LogLevel.DEBUG) {
            log.lifecycle(message())
        }
    }

    fun info(message: () -> String) {
        if (logLevel <= LogLevel.INFO) {
            log.lifecycle(message())
        }
    }

    fun lifecycle(message: () -> String) {
        if (logLevel <= LogLevel.LIFECYCLE) {
            log.lifecycle(message())
        }
    }

    fun warn(message: () -> String) {
        if (logLevel <= LogLevel.WARN) {
            log.lifecycle(message())
        }
    }

    fun quiet(message: () -> String) {
        if (logLevel <= LogLevel.QUIET) {
            log.lifecycle(message())
        }
    }

    fun error(message: () -> String) = log.error(message())

    companion object {
        const val logLevelProperty = "kotlinx.fuzz.logging.level"
        private val logLevel: LogLevel = LogLevel.valueOf(System.getProperty(logLevelProperty).uppercase())
    }
}
