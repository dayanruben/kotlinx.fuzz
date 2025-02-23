package kotlinx.fuzz.config

interface CoverageConfig {
    val reportTypes: Set<CoverageReportType>
    val includeDependencies: Set<String>
}

class CoverageConfigImpl internal constructor(builder: KFConfigBuilder) : CoverageConfig {
    override var reportTypes by builder.KFPropProvider<Set<CoverageReportType>>(
        nameSuffix = "coverage.reportTypes",
        intoString = { it.toString() },
        fromString = { it.split(",").map { CoverageReportType.valueOf(it) }.toSet() },
        default = setOf(CoverageReportType.HTML)
    )

    override var includeDependencies by builder.KFPropProvider<Set<String>>(
        nameSuffix = "coverage.includeDependencies",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").toSet() },
        default = emptySet(),
    )
}

enum class CoverageReportType {
    HTML, XML, CSV;
}
