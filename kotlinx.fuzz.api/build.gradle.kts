plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("org.junit.platform:junit-platform-engine:$JUNIT_PLATFORM_VERSION")
    implementation(project(":kotlinx.fuzz.jazzer"))
}

tasks.test {
    useJUnitPlatform()
}
