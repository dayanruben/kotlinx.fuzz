package kotlinx.fuzz.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface TargetConfig {
    val maxTime: Duration

    object Defaults {
        val MAX_TIME = 1.minutes
    }
}

class TargetConfigImpl internal constructor(builder: KFConfigBuilder) : TargetConfig {

    override var maxTime by builder.KFPropProvider<Duration>(
        nameSuffix = "maxTime",
        intoString = { it.toString() },
        fromString = { Duration.parse(it) },
        validate = { require(it.isPositive()) { "maxTime must be positive!" } },
        default = Duration.parse("1m"),
    )
}
