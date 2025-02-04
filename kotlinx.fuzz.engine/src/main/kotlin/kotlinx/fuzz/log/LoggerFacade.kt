package kotlinx.fuzz.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object LoggerFacade {
    const val LOG_LEVEL_PROPERTY = "kotlinx.fuzz.log.level"
    val LOG_LEVEL = System.getProperty(LOG_LEVEL_PROPERTY, Level.WARN.toString()).uppercase()
        .let { levelName -> Level.entries.first { it.toString() == levelName } }
    private val isSlf4jAvailable: Boolean by lazy {
        val slf4jProviders = this::class.java.classLoader.getResource("org.slf4j.spi.SLF4JServiceProvider")
            ?.readText()
            .orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }
        slf4jProviders.isNotEmpty()
    }

    fun getLogger(name: String): Logger = when {
        isSlf4jAvailable -> LoggerFactory.getLogger(name)
        else -> DefaultSlf4jLogger(name)
    }

    inline fun <reified T> getLogger(): Logger = getLogger(T::class.java.name)
}
