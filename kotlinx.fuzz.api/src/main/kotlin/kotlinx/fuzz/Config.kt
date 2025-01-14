package kotlinx.fuzz

import kotlinx.fuzz.jazzer.JazzerConfig

data class Config(
    val fuzzEngine: String,
    val jazzerConfig: JazzerConfig
) {
    companion object {
        fun fromSystemProperties(): Config {
            return Config(
                fuzzEngine = System.getProperty("kotlinx.fuzz.engine", "jazzer"),
                jazzerConfig = JazzerConfig.fromSystemProperties()
            )
        }
    }
}
