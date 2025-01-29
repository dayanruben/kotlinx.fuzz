package kotlinx.fuzz

import kotlin.reflect.full.primaryConstructor

@Suppress("OBJECT_NAME_INCORRECT")
object KLoggerFactory {
    const val LOGGER_IMPLEMENTATION_PROPERTY = "kotlinx.fuzz.logger.implementation"

    fun getLogger(clazz: Class<*>): KLogger {
        val loggerImplementation = System.getProperty(LOGGER_IMPLEMENTATION_PROPERTY, DefaultLogger::class.qualifiedName!!)
        return Class.forName(loggerImplementation)
            .kotlin
            .primaryConstructor!!
            .call(clazz) as KLogger
    }
}

abstract class KLogger(clazz: Class<*>) {
    abstract fun debug(message: () -> String)
    abstract fun info(message: () -> String)
    abstract fun warn(message: () -> String)
    abstract fun error(message: () -> String)
}

class DefaultLogger(clazz: Class<*>) : KLogger(clazz) {
    override fun debug(message: () -> String) {}
    override fun info(message: () -> String) {}
    override fun warn(message: () -> String) {}
    override fun error(message: () -> String) {}
}
