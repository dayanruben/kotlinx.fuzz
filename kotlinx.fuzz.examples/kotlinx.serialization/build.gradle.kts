plugins {
    id("kotlinx.fuzz.example-module")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation(libs.jazzer.api)
    implementation(libs.jazzer.junit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.kotlinx.serialization.protobuf)
}
