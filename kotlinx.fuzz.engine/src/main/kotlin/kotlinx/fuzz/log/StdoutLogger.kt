package kotlinx.fuzz.log

import java.time.LocalDateTime
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level

/**
 * Basic slf4j logger implementation that works as a fallback when there are no slf4j service providers.
 * By default, all the levels are enabled.
 */
@LoggerImpl
internal object StdoutLogger : Logger {
    private fun log(level: Level, msg: String?, t: Throwable? = null) {
        val timestamp = LocalDateTime.now()
        val threadName = Thread.currentThread().name
        val message = "$timestamp [$threadName] [$level] - ${msg ?: ""}"
        println(message)
        t?.printStackTrace()
    }

    private fun isLevelEnabled(level: Level): Boolean = true

    override fun getName(): String = "stdout"

    override fun isTraceEnabled(): Boolean = isLevelEnabled(Level.TRACE)
    override fun trace(msg: String?) = log(Level.TRACE, msg)
    override fun trace(format: String?, arg: Any?) = log(Level.TRACE, format?.format(arg))
    override fun trace(format: String?, arg1: Any?, arg2: Any?) = log(Level.TRACE, format?.format(arg1, arg2))
    override fun trace(format: String?, vararg arguments: Any?) = log(Level.TRACE, format?.format(*arguments))
    override fun trace(msg: String?, t: Throwable?) = log(Level.TRACE, msg, t)

    override fun isTraceEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.TRACE)
    override fun trace(marker: Marker?, msg: String?) = trace(msg)
    override fun trace(marker: Marker?, format: String?, arg: Any?) = trace(format, arg)
    override fun trace(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = trace(format, arg1, arg2)
    override fun trace(marker: Marker?, format: String?, vararg arguments: Any?) = trace(format, *arguments)
    override fun trace(marker: Marker?, msg: String?, t: Throwable?) = trace(msg, t)

    override fun isDebugEnabled(): Boolean = isLevelEnabled(Level.DEBUG)
    override fun debug(msg: String?) = log(Level.DEBUG, msg)
    override fun debug(format: String?, arg: Any?) = log(Level.DEBUG, format?.format(arg))
    override fun debug(format: String?, arg1: Any?, arg2: Any?) = log(Level.DEBUG, format?.format(arg1, arg2))
    override fun debug(format: String?, vararg arguments: Any?) = log(Level.DEBUG, format?.format(*arguments))
    override fun debug(msg: String?, t: Throwable?) = log(Level.DEBUG, msg, t)

    override fun isDebugEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.DEBUG)
    override fun debug(marker: Marker?, msg: String?) = debug(msg)
    override fun debug(marker: Marker?, format: String?, arg: Any?) = debug(format, arg)
    override fun debug(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = debug(format, arg1, arg2)
    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) = debug(format, *arguments)
    override fun debug(marker: Marker?, msg: String?, t: Throwable?) = debug(msg, t)

    override fun isInfoEnabled(): Boolean = isLevelEnabled(Level.INFO)
    override fun info(msg: String?) = log(Level.INFO, msg)
    override fun info(format: String?, arg: Any?) = log(Level.INFO, format?.format(arg))
    override fun info(format: String?, arg1: Any?, arg2: Any?) = log(Level.INFO, format?.format(arg1, arg2))
    override fun info(format: String?, vararg arguments: Any?) = log(Level.INFO, format?.format(*arguments))
    override fun info(msg: String?, t: Throwable?) = log(Level.INFO, msg, t)

    override fun isInfoEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.INFO)
    override fun info(marker: Marker?, msg: String?) = info(msg)
    override fun info(marker: Marker?, format: String?, arg: Any?) = info(format, arg)
    override fun info(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = info(format, arg1, arg2)
    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) = info(format, *arguments)
    override fun info(marker: Marker?, msg: String?, t: Throwable?) = info(msg, t)

    override fun isWarnEnabled(): Boolean = LoggerFacade.LOG_LEVEL.toInt() <= Level.WARN.toInt()
    override fun warn(msg: String?) = log(Level.WARN, msg)
    override fun warn(format: String?, arg: Any?) = log(Level.WARN, format?.format(arg))
    override fun warn(format: String?, arg1: Any?, arg2: Any?) = log(Level.WARN, format?.format(arg1, arg2))
    override fun warn(format: String?, vararg arguments: Any?) = log(Level.WARN, format?.format(*arguments))
    override fun warn(msg: String?, t: Throwable?) = log(Level.WARN, msg, t)

    override fun isWarnEnabled(marker: Marker?): Boolean = LoggerFacade.LOG_LEVEL.toInt() <= Level.WARN.toInt()
    override fun warn(marker: Marker?, msg: String?) = warn(msg)
    override fun warn(marker: Marker?, format: String?, arg: Any?) = warn(format, arg)
    override fun warn(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = warn(format, arg1, arg2)
    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) = warn(format, *arguments)
    override fun warn(marker: Marker?, msg: String?, t: Throwable?) = warn(msg, t)

    override fun isErrorEnabled(): Boolean = LoggerFacade.LOG_LEVEL.toInt() <= Level.ERROR.toInt()
    override fun error(msg: String?) = log(Level.ERROR, msg)
    override fun error(format: String?, arg: Any?) = log(Level.ERROR, format?.format(arg))
    override fun error(format: String?, arg1: Any?, arg2: Any?) = log(Level.ERROR, format?.format(arg1, arg2))
    override fun error(format: String?, vararg arguments: Any?) = log(Level.ERROR, format?.format(*arguments))
    override fun error(msg: String?, t: Throwable?) = log(Level.ERROR, msg, t)

    override fun isErrorEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.ERROR)
    override fun error(marker: Marker?, msg: String?) = error(msg)
    override fun error(marker: Marker?, format: String?, arg: Any?) = error(format, arg)
    override fun error(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = error(format, arg1, arg2)
    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) = error(format, *arguments)
    override fun error(marker: Marker?, msg: String?, t: Throwable?) = error(msg, t)
}
