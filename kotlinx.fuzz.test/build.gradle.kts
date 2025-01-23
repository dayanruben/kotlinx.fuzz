import kotlinx.fuzz.gradle.fuzzConfig
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz")
}

repositories {
    mavenCentral()
}

dependencies {
    testRuntimeOnly("kotlinx.fuzz:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.fuzz.test.**")
    maxSingleTargetFuzzTime = 10.seconds
}

kotlin {
    jvmToolchain(17)
}
