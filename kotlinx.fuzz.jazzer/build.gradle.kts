import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.jazzer)
}

configurePublishing()
