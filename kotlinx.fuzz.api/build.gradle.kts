plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("com.github.curious-odd-man:rgxgen:$RGX_GEN_VERSION")
    implementation("org.junit.platform:junit-platform-engine:$JUNIT_PLATFORM_VERSION")
}

tasks.test {
    useJUnitPlatform()
}
