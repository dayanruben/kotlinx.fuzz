package kotlinx.fuzz.jazzer

import kotlinx.fuzz.SystemProperty

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
            libFuzzerRssLimit = SystemProperty.JAZZER_LIBFUZZERARGS_RSS_LIMIT_MB.get("0").toInt(),
            enableLogging = SystemProperty.JAZZER_ENABLE_LOGGING.get("false").toBooleanStrict(),
        )
    }
}
