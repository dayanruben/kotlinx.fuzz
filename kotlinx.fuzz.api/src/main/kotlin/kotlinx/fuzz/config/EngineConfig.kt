package kotlinx.fuzz.config

private const val NAME_PREFIX = "jazzer"

sealed interface EngineConfig

interface JazzerConfig : EngineConfig {
    val libFuzzerRssLimitMb: Int

    /**
     * Maximum heap size for the fuzzer, specified in megabytes. Default: 4096
     */
    val maxHeapSizeMb: Long

    object Defaults {
        const val MAX_HEAP_SIZE_MB = 4096L
    }
}

class JazzerConfigImpl internal constructor(builder: KFuzzConfigBuilder) : JazzerConfig {
    override var libFuzzerRssLimitMb: Int by builder.KFuzzPropProvider(
        "$NAME_PREFIX.libFuzzerArgs.rssLimitMb",
        intoString = { it.toString() },
        fromString = { it.toInt() },
        validate = { require(it >= 0) { "rssLimit must be positive!" } },
        default = 0,
    )
    override var maxHeapSizeMb: Long by builder.KFuzzPropProvider(
        "$NAME_PREFIX.maxHeapSizeMb",
        default = JazzerConfig.Defaults.MAX_HEAP_SIZE_MB,
        intoString = { it.toString() },
        fromString = { it.toLong() },
        validate = { require(it > 0) { "maxHeapSize must greater than 0!" } },
    )
}
