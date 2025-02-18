package kotlinx.fuzz.config

import kotlin.time.Duration

interface TargetConfig {
    val maxTime: Duration
}

class TargetConfigImpl internal constructor(
    override val builder: KFConfigBuilder
) : TargetConfig, KFConfigHolder {

    override var maxTime by kfProperty<Duration>(
        nameSuffix = "maxTime",
        intoString = { it.toString() },
        fromString = { Duration.parse(it) },
        validate = { require(it.isPositive()) { "maxTime must be positive!" } },
        default = Duration.parse("1m"),
    )
}
