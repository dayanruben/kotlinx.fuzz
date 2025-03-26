import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.hours

plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
    kotlin("plugin.serialization") version "2.0.20"
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("plan-jazzer-api").get())
    implementation(libs.findLibrary("plan-jazzer-junit").get())
    implementation(libs.findLibrary("kotlinx-serialization-json-examples").get())
    implementation(libs.findLibrary("kotlinx-serialization-cbor-examples").get())
    implementation(libs.findLibrary("kotlinx-serialization-properties-examples").get())
    implementation(libs.findLibrary("kotlinx-serialization-protobuf-examples").get())

    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.serialization.**")
    maxFuzzTimePerTarget = 8.hours
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        includeDependencies = listOf(
            libs.findLibrary("kotlinx-serialization-json-examples").get(),
            libs.findLibrary("kotlinx-serialization-cbor-examples").get(),
            libs.findLibrary("kotlinx-serialization-properties-examples").get(),
            libs.findLibrary("kotlinx-serialization-protobuf-examples").get()
        )
            .map { it.get() }
            .map { "${it.group}:${it.name}-jvm" }
            .toSet()
    }
}
