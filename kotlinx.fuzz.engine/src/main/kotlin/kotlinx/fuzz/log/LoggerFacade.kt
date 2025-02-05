package kotlinx.fuzz.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * Custom logger facade that uses slf4j service provider if available and falls back to StdoutLogger if not
 */
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

    fun getLogger(name: String): Logger = LoggerWrapper(
        when {
            isSlf4jAvailable -> LoggerFactory.getLogger(name)
            else -> StdoutLogger
        },
    )

    inline fun <reified T> getLogger(): Logger = getLogger(T::class.java.name)
}
