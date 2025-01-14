package kotlinx.fuzz

data class FuzzConfig(
    val fuzzEngine: String,
    val hooks: Boolean,
    val instrument: List<String>,
    val customHookExcludes: List<String>,
    val maxTotalTime: Int,
) {
    companion object {
        fun fromSystemProperties(): FuzzConfig {
            return FuzzConfig(
                fuzzEngine = System.getProperty("kotlinx.fuzz.engine", "jazzer"),
                hooks = System.getProperty("kotlinx.fuzz.hooks")?.toBoolean() ?: false,
                instrument = System.getProperty("kotlinx.fuzz.instrument")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                customHookExcludes = System.getProperty("kotlinx.fuzz.customHookExcludes")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                maxTotalTime = System.getProperty("kotlinx.fuzz.maxTotalTime", "10").toInt()
            )
        }
    }
}
