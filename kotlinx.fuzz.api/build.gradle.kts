plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(libs.rgxgen)
    implementation(libs.junit.platform.engine)
}

tasks.test {
    useJUnitPlatform()
}
