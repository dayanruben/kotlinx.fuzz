package kotlinx.fuzz.log

import org.slf4j.Logger

@LoggerImpl
fun Logger.trace() = this.trace("")
fun <T> Logger.trace(t: T) = this.trace(t.toString())

@LoggerImpl
fun Logger.info() = this.info("")
fun <T> Logger.info(t: T) = this.info(t.toString())

@LoggerImpl
fun Logger.debug() = this.debug("")
fun <T> Logger.debug(t: T) = this.debug(t.toString())

@LoggerImpl
fun Logger.warn() = this.warn("")
fun <T> Logger.warn(t: T) = this.warn(t.toString())

@LoggerImpl
fun Logger.error() = this.error("")
fun <T> Logger.error(t: T) = this.error(t.toString())

inline fun Logger.debug(message: () -> String) = when {
    isDebugEnabled -> debug(message())
    else -> {}
}

inline fun Logger.trace(message: () -> String) = when {
    isTraceEnabled -> trace(message())
    else -> {}
}

inline fun Logger.info(message: () -> String) = when {
    isInfoEnabled -> info(message())
    else -> {}
}

inline fun Logger.warn(message: () -> String) = when {
    isWarnEnabled -> warn(message())
    else -> {}
}

inline fun Logger.error(message: () -> String) = when {
    isErrorEnabled -> error(message())
    else -> {}
}
