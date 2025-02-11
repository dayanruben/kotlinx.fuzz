package kotlinx.fuzz.jazzer

import kotlinx.fuzz.SystemProperties

/**
 * Jazzer specific configuration properties
 *
 * @param libFuzzerRssLimit rss limit in MB, 0 by default
 * @param enableLogging flag to enable jazzer logs, false by default
 */
data class JazzerConfig(
    val libFuzzerRssLimit: Int,
    val enableLogging: Boolean,
) {
    companion object {
        fun fromSystemProperties(): JazzerConfig = JazzerConfig(
            libFuzzerRssLimit = System.getProperty(SystemProperties.JAZZER_LIBFUZZERARGS_RSS_LIMIT_MB, "0").toInt(),
            enableLogging = System.getProperty(SystemProperties.JAZZER_ENABLE_LOGGING, "false").toBooleanStrict(),
        )
    }
}
