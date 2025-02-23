package kotlinx.fuzz.config

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

interface GlobalConfig {
    val workDir: Path
    val reproducerDir: Path
    val hooks: Boolean
    val logLevel: LogLevel
    val regressionEnabled: Boolean
}

class GlobalConfigImpl internal constructor(builder: KFConfigBuilder) : GlobalConfig {

    override var workDir by builder.KFPropProvider<Path>(
        nameSuffix = "workDir",
        fromString = { Path.of(it) },
        intoString = { it.absolutePathString() },
    )

    override var logLevel by builder.KFPropProvider<LogLevel>(
        nameSuffix = "logLevel",
        intoString = { it.toString() },
        fromString = { LogLevel.valueOf(it) },
        default = LogLevel.WARN,
    )

    override var reproducerDir by builder.KFPropProvider<Path>(
        nameSuffix = "reproducerDir",
        intoString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )

    override var hooks by builder.KFPropProvider<Boolean>(
        nameSuffix = "enableHooks",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = true,
    )

    override var regressionEnabled by builder.KFPropProvider<Boolean>(
        nameSuffix = "regression",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
}

enum class LogLevel {
    ERROR, WARN, INFO, DEBUG, TRACE;
}
