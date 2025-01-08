plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    val junitPlatformVersion = "1.11.4"
    implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    implementation(kotlin("reflect")) // FIXME: remove after being able to run tests with jazzer
}


tasks.test {
    useJUnitPlatform()
}