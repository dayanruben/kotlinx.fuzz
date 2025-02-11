import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))
    implementation(project(":kotlinx.fuzz.CasrAdapter"))
    implementation(kotlin("reflect"))
    implementation(libs.plan.jazzer)
    implementation(libs.slf4j.api)
}

configurePublishing()
