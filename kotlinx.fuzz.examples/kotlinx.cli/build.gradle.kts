import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.seconds

plugins {
    id("kotlinx.fuzz.example-module")
    id("org.jetbrains.kotlinx.fuzz")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.kotlinx.cli)
    implementation(libs.commons.cli)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.cli.**")
    maxFuzzTimePerTarget = 10.seconds // TODO
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
//    detailedLogging = true
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        val immutable = libs.kotlinx.cli.get()
        includeDependencies = setOf("${immutable.group}:${immutable.name}-jvm")
    }
}
