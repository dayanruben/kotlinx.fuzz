import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.platform:junit-platform-engine:1.11.4")
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
//    testImplementation("kotlinx.fuzz:kotlinx.fuzz.gradle:0.0.1")
}

kfuzz.config {
    instrument = listOf("kotlinx.fuzz.test.**")
    maxSingleTargetFuzzTime = 10.seconds
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}