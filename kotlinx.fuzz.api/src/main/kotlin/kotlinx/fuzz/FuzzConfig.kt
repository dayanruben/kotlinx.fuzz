package kotlinx.fuzz

data class FuzzConfig(
    val fuzzEngine: String
) {
    companion object {
        fun fromSystemProperties(): FuzzConfig {
            return FuzzConfig(
                fuzzEngine = System.getProperty("kotlinx.fuzz.engine", "jazzer")
            )
        }
    }
}
