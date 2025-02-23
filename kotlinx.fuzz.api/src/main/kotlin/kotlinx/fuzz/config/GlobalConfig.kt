package kotlinx.fuzz.config

import java.nio.file.Path
import kotlin.io.path.absolutePathString

interface GlobalConfig {
    val workDir: Path
}

class GlobalConfigImpl internal constructor(builder: KFConfigBuilder) : GlobalConfig {

    override var workDir by builder.KFPropProvider<Path>(
        nameSuffix = "workDir",
        fromString = { Path.of(it) },
        intoString = { it.absolutePathString() },
    )
}
