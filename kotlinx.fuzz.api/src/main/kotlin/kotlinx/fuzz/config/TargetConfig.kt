package kotlinx.fuzz.config

import kotlin.time.Duration

interface TargetConfig {
    val maxFuzzTime: Duration
    val keepGoing: Long
    val instrument: List<String>
    val customHookExcludes: List<String>
    val dumpCoverage: Boolean

    object Defaults {
        const val MAX_FUZZ_TIME_STRING = "1m"
        const val KEEP_GOING = 0L
        const val DUMP_COVERAGE = true
    }
}

class TargetConfigImpl internal constructor(builder: KFConfigBuilder) : TargetConfig {
    override var maxFuzzTime by builder.KFPropProvider<Duration>(
        nameSuffix = "maxFuzzTimePerTarget",
        intoString = { it.toString() },
        fromString = { Duration.parse(it) },
        validate = { require(it.isPositive()) { "maxTime must be positive" } },
        default = Duration.parse(TargetConfig.Defaults.MAX_FUZZ_TIME_STRING),
    )

    override var keepGoing by builder.KFPropProvider<Long>(
        nameSuffix = "keepGoing",
        intoString = { it.toString() },
        fromString = { it.toLong() },
        validate = { require(it >= 0) { "keepGoing must be non-negative" } },
        default = TargetConfig.Defaults.KEEP_GOING,
    )

    override var instrument by builder.KFPropProvider<List<String>>(
        nameSuffix = "instrument",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",") },
    )

    override var customHookExcludes by builder.KFPropProvider<List<String>>(
        nameSuffix = "customHookExcludes",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",") },
        default = emptyList(),
    )

    override var dumpCoverage by builder.KFPropProvider<Boolean>(
        nameSuffix = "dumpCoverage",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = TargetConfig.Defaults.DUMP_COVERAGE,
    )
}
