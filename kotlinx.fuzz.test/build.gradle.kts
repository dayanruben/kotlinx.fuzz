import kotlinx.fuzz.gradle.fuzzConfig
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlinx.fuzz")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test")) // adds green arrow in IDEA (no idea why)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.fuzz.test.**")
    maxSingleTargetFuzzTime = 10.seconds
}

kotlin {
    jvmToolchain(17)
}
