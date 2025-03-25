package kotlinx.fuzz.config

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

private const val NAME_PREFIX = "coverage"

interface CoverageConfig {
    val reportTypes: Set<CoverageReportType>
    val includeDependencies: Set<String>
    val jacocoAgentPath: Path
}

class CoverageConfigImpl internal constructor(builder: KFuzzConfigBuilder) : CoverageConfig {
    override var reportTypes: Set<CoverageReportType> by builder.KFuzzPropProvider(
        nameSuffix = "$NAME_PREFIX.reportTypes",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").map { CoverageReportType.valueOf(it) }.toSet() },
        default = setOf(CoverageReportType.HTML),
    )
    override var includeDependencies: Set<String> by builder.KFuzzPropProvider(
        nameSuffix = "$NAME_PREFIX.includeDependencies",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").toSet() },
        default = emptySet(),
    )
    override var jacocoAgentPath: Path by builder.KFuzzPropProvider(
        default = Path("/Users/ilma4/Downloads/jacoco-0.8.12/lib/jacocoagent.jar"),
        nameSuffix = "$NAME_PREFIX.jacocoAgentPath",
        intoString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )
}

enum class CoverageReportType {
    CSV, HTML, XML;
}
