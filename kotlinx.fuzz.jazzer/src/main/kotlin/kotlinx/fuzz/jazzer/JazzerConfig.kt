package kotlinx.fuzz.jazzer

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
            libFuzzerRssLimit = System.getProperty("kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb", "0").toInt(),
            enableLogging = System.getProperty("kotlinx.fuzz.jazzer.enableLogging", "false").toBooleanStrict(),
        )
    }
}
