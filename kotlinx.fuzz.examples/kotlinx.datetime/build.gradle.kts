import kotlin.time.Duration.Companion.minutes

plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.jazzer.api)
    implementation(libs.jazzer.junit)
    implementation(libs.reflections)

    testImplementation(libs.kotlinx.datetime)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}


fuzzConfig {
    instrument = listOf("kotlinx.datetime.**")
    maxFuzzTimePerTarget = 1.minutes
    supportJazzerTargets = true
    coverage {
        includeDependencies = setOf(libs.kotlinx.datetime.get().toString())
    }
}