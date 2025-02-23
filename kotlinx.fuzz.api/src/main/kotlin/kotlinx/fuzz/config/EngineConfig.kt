package kotlinx.fuzz.config

sealed interface EngineConfig

interface JazzerConfig : EngineConfig {
    val libFuzzerRssLimitMb: Int
    val enableLogging: Boolean

    object Defaults {
        const val LIB_FUZZER_RSS_LIMIT = 0
        const val ENABLE_LOGGING = false
    }
}

class JazzerConfigImpl internal constructor(builder: KFConfigBuilder) : JazzerConfig {

    override var libFuzzerRssLimitMb by builder.KFPropProvider<Int>(
        "jazzer.libFuzzerArgs.rssLimitMb",
        intoString = { it.toString() },
        fromString = { it.toInt() },
        validate = { require(it >= 0) { "rssLimit must be positive!" } },
        default = 0,
    )

    override var enableLogging by builder.KFPropProvider<Boolean>(
        "jazzer.enableLogging",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
}