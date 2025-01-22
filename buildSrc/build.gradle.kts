plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(kotlin("reflect"))
    implementation(libs.reflections)
}

kotlin {
    jvmToolchain(17)
}
