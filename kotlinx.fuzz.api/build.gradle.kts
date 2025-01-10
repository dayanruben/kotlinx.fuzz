plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

tasks.test {
    useJUnitPlatform()
}
