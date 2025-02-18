package kotlinx.fuzz.config

import java.nio.file.Path
import kotlin.io.path.absolutePathString

interface GlobalConfig {
    val workDir: Path
}

class GlobalConfigImpl internal constructor(
    override val builder: KFConfigBuilder
) : GlobalConfig, KFConfigHolder {

    override var workDir by kfProperty<Path>(
        nameSuffix = "workDir",
        fromString = { Path.of(it) },
        intoString = { it.absolutePathString() },
    )
}
