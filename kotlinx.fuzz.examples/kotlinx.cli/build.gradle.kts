import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.seconds

plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.findLibrary("kotlinx-cli").get())
    implementation(libs.findLibrary("commons-cli").get())
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
        val immutable = libs.findLibrary("kotlinx-cli").get().get()
        includeDependencies = setOf("${immutable.group}:${immutable.name}-jvm")
    }
}
