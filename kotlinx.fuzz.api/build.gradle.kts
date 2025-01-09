plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}
