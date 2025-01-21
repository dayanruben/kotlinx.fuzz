plugins {
    id("kotlinx.fuzz.src-module")
    `kotlin-dsl`
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))

    gradleApi()
    implementation(kotlin("reflect"))
    implementation(libs.junit.platform.engine)
    implementation(libs.junit.platform.testkit)
    implementation(libs.junit.jupiter)
    implementation(libs.jazzer.api)
    testRuntimeOnly(project(":kotlinx.fuzz.jazzer"))
}

gradlePlugin.plugins.create("kotlinx.fuzz") {
    id = "kotlinx.fuzz.gradle"
    implementationClass = "kotlinx.fuzz.gradle.KFuzzPlugin"
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}
