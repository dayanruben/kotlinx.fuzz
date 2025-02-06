import kotlinx.fuzz.RunMode
import kotlinx.fuzz.JacocoReport.*
import kotlinx.fuzz.gradle.fuzzConfig
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz.gradle")
    kotlin("plugin.serialization") version "2.0.20"
}

repositories {
    mavenCentral()
    maven(url = "https://plan-maven.apal-research.com")
}

dependencies {
    testImplementation(kotlin("test")) // adds green arrow in IDEA (no idea why)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
}

fuzzConfig {
    keepGoing = 2
    runModes = setOf(RunMode.FUZZING)
    instrument = listOf(
        "kotlinx.fuzz.test.**",
        "kotlinx.collections.immutable.**",
        "kotlinx.serialization.**",
    )
    maxSingleTargetFuzzTime = 10.seconds
    jacocoReports = setOf(HTML, CSV, XML)
    jacocoReportIncludedDependencies = setOf(
        "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm",
        "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm",
    )
}

kotlin {
    jvmToolchain(17)
}
