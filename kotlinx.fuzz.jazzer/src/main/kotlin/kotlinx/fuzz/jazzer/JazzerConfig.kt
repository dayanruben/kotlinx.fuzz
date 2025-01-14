package kotlinx.fuzz.jazzer

data class JazzerConfig(
    val hooks: Boolean,
    val instrument: List<String>,
    val customHookExcludes: List<String>,
    val libFuzzerMaxTotalTime: Int,
    val libFuzzerRssLimit: Int // TODO: Other settings
) {
    companion object {
        fun fromSystemProperties(): JazzerConfig {
            return JazzerConfig(
                hooks = System.getProperty("jazzer.hooks")?.toBoolean() ?: false,
                instrument = System.getProperty("jazzer.instrument")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                customHookExcludes = System.getProperty("jazzer.customHookExcludes")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                libFuzzerMaxTotalTime = System.getProperty("jazzer.libFuzzerArgs.max_total_time", "10").toInt(),
                libFuzzerRssLimit = System.getProperty("jazzer.libFuzzerArgs.rss_limit_mb", "0").toInt()
            )
        }
    }
}
