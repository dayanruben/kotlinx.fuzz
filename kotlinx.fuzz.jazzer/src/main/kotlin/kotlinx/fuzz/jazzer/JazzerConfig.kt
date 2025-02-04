package kotlinx.fuzz.jazzer

data class JazzerConfig(
    val libFuzzerRssLimit: Int,  // TODO: Other settings
    val enableLogging: Boolean,
) {
    companion object {
        fun fromSystemProperties(): JazzerConfig = JazzerConfig(
            libFuzzerRssLimit = System.getProperty("kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb", "0").toInt(),
            enableLogging = System.getProperty("kotlinx.fuzz.jazzer.enableLogging", "false").toBooleanStrict(),
        )
    }
}
