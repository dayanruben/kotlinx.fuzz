plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("org.junit.platform:junit-platform-engine:$JUNIT_PLATFORM_VERSION")
    implementation(kotlin("reflect"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter:$JUNIT_JUPITER_VERSION")
}

tasks.test {
    useJUnitPlatform()
}
