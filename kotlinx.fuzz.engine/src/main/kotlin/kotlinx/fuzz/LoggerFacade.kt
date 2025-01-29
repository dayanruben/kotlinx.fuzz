package kotlinx.fuzz

import kotlin.reflect.full.primaryConstructor

object KLoggerFactory {
    const val LOGGER_IMPLEMENTATION_PROPERTY = "kotlinx.fuzz.logger.implementation"

    private var loggerImplementation: String? = System.getProperty(LOGGER_IMPLEMENTATION_PROPERTY)

    fun getLogger(clazz: Class<*>): KLogger =
        loggerImplementation?.let {
            try {
                val loggerClass = Class.forName(it).kotlin
                val constructor = loggerClass.primaryConstructor
                if (constructor != null) {
                    constructor.call(clazz) as? KLogger
                } else {
                    loggerClass.objectInstance as? KLogger
                }
            } catch (e: Exception) {
                println("Failed to load logger implementation: $it, using DefaultLogger. Error: ${e.message}")
                DefaultLogger(clazz)
            }
        } ?: DefaultLogger(clazz)
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
