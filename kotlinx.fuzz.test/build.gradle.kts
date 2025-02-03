import kotlinx.fuzz.JacocoReport.*
import kotlinx.fuzz.gradle.fuzzConfig
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz.gradle")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test")) // adds green arrow in IDEA (no idea why)
    testImplementation("org.junit.platform:junit-platform-testkit:1.11.4")
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.fuzz.test.**")
    maxSingleTargetFuzzTime = 10.seconds
    jacocoReports = setOf(HTML, CSV, XML)
}

kotlin {
    jvmToolchain(17)
}
