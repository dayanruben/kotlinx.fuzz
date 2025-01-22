plugins {
    id("kotlinx.fuzz.example-module")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.jazzer.api)
    implementation(libs.jazzer.junit)
    implementation(libs.kotlinx.cli)
    implementation(libs.commons.cli)
}
