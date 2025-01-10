plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    val junitPlatformVersion = "1.11.4"
    implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    val jazzerVersion = "0.22.1"
    implementation("com.code-intelligence:jazzer:$jazzerVersion")
    implementation(kotlin("reflect"))

    testImplementation("com.code-intelligence:jazzer-api:$jazzerVersion")
    testImplementation("org.junit.platform:junit-platform-testkit:$junitPlatformVersion")
}


tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz-test")
    }
}