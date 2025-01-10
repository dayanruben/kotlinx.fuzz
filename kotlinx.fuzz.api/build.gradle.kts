plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("com.github.curious-odd-man:rgxgen:2.0")
    implementation("org.junit.platform:junit-platform-engine:$JUNIT_PLATFORM_VERSION")
}

tasks.test {
    useJUnitPlatform()
}
