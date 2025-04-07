import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.hours

plugins {
    id("kotlinx.fuzz.example-module")
    id("org.jetbrains.kotlinx.fuzz")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.kotlinx.serialization.json.examples)
    implementation(libs.kotlinx.serialization.cbor.examples)
    implementation(libs.kotlinx.serialization.properties.examples)
    implementation(libs.kotlinx.serialization.protobuf.examples)

    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.serialization.**")
    maxFuzzTimePerTarget = 8.hours
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        includeDependencies = with(libs.kotlinx.serialization) {
            listOf(json.examples, cbor.examples, properties.examples, protobuf.examples)
                .map { it.get() }
                .map { "${it.group}:${it.name}-jvm" }
                .toSet()
        }
    }
}
