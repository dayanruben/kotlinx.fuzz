plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    val junitPlatformVersion = "1.11.4"
    implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    val jazzerVersion = "0.22.1"
//    val jazzerVersion = "0.23.0"
    implementation("com.code-intelligence:jazzer:$jazzerVersion")
    testImplementation("com.code-intelligence:jazzer-api:$jazzerVersion")
    implementation(kotlin("reflect")) // FIXME: remove after being able to run tests with jazzer
}


tasks.test {
    useJUnitPlatform()
}