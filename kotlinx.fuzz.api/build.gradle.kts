plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(libs.rgxgen)
    implementation(libs.junit.platform.engine)
    implementation(kotlin("reflect"))
    implementation(gradleApi())

    testRuntimeOnly(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
