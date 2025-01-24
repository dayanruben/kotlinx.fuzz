plugins {
    id("kotlinx.fuzz.src-module")
    `kotlin-dsl`
}

dependencies {
    implementation(libs.rgxgen)
    implementation(libs.junit.platform.engine)
    implementation(kotlin("reflect"))

    implementation(libs.junit.platform.engine)

    testRuntimeOnly(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
